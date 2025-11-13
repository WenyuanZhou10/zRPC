package cn.wenyuan.zrpc.server.old_version;

import cn.wenyuan.zrpc.core.server.RpcServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A minimal non-blocking RPC server implementation that uses a single {@link Selector}
 * to accept connections, read framed requests, and write back responses.
 *
 * <p>The wire format is a simple length-prefixed frame:
 * <pre>
 *   | length (4 bytes, big endian) | payload bytes |
 * </pre>
 * The payload in this demo implementation is treated as UTF-8 text.
 */
public class NIOServer implements RpcServer {

    private static final int SELECT_TIMEOUT_MILLIS = 500;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Selector selector;
    private ServerSocketChannel serverChannel;
    private Thread selectorThread;

    @Override
    public synchronized void start(int port) {
        if (running.get()) {
            throw new IllegalStateException("NIO server already started");
        }

        try {
            selector = Selector.open();
            serverChannel = ServerSocketChannel.open(); // 专门用来处理连接事件的 Channel
            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            running.set(true);
            selectorThread = new Thread(this::runEventLoop, "zrpc-nio-selector");
            selectorThread.setDaemon(true);
            selectorThread.start();

            System.out.println("NIO server start on port " + port + "!");
        } catch (IOException e) {
            running.set(false);
            closeQuietly(serverChannel);
            closeQuietly(selector);
            throw new RuntimeException("nio server start failed", e);
        }
    }

    private void runEventLoop() {
        try {
            while (running.get()) {
                try {
                    selector.select(SELECT_TIMEOUT_MILLIS);
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectedKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        if (!key.isValid()) {
                            continue;
                        }

                        try {
                            if (key.isAcceptable()) {
                                handleAccept(key);
                            }
                            if (key.isReadable()) {
                                handleRead(key);
                            }
                            if (key.isWritable()) {
                                handleWrite(key);
                            }
                        } catch (IOException ioException) {
                            System.err.println("client handling error: " + ioException.getMessage());
                            closeKey(key);
                        }
                    }
                } catch (CancelledKeyException ignore) {
                    // Key cancelled while iterating; continue loop to pick up remaining keys.
                }
            }
        } catch (IOException selectorError) {
            if (running.get()) {
                System.err.println("selector loop error: " + selectorError.getMessage());
            }
        } finally {
            cleanup();
        }
    }

    private void handleAccept(SelectionKey serverKey) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) serverKey.channel();
        SocketChannel client = server.accept();
        if (client == null) {
            return;
        }
        client.configureBlocking(false);
        SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ);
        clientKey.attach(new ClientSession());
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ClientSession session = getOrCreateSession(key);

        ensureReadBufferCapacity(session); // 缓冲区扩容
        ByteBuffer buffer = session.readBuffer;

        int bytesRead = channel.read(buffer); // 数据读入缓冲区
        if (bytesRead == -1) { // 客户端关闭连接
            closeKey(key);
            return;
        }
        if (bytesRead == 0) {
            return;
        }

        buffer.flip(); // 切换为读模式
        while (true) {
            if (session.expectedFrameSize < 0) { // 等待帧头
                if (buffer.remaining() >= Integer.BYTES) { // 是否 >= 4字节
                    session.expectedFrameSize = buffer.getInt(); // 读取 4 字节的长度。并为这个帧分配一个精确大小的 frameBuffer。
                    if (session.expectedFrameSize <= 0) {
                        throw new IOException("invalid frame length: " + session.expectedFrameSize);
                    }
                    session.frameBuffer = ByteBuffer.allocate(session.expectedFrameSize);
                } else { // 如果不是，说明是半包，要等到后续数据
                    break;
                }
            }

            if (session.frameBuffer != null) { // 可以开始接收帧头
                // Socket 发来的数据、当前帧需要的数据量，两者取最小值来获取
                // 解决 半包 问题 和 黏包 问题
                int toTransfer = Math.min(buffer.remaining(), session.frameBuffer.remaining());
                if (toTransfer > 0) {
                    byte[] slice = new byte[toTransfer];
                    buffer.get(slice);
                    session.frameBuffer.put(slice);
                }

                if (!session.frameBuffer.hasRemaining()) { // 当前 frameBuffer 是否满了，满了表示完整收到
                    session.frameBuffer.flip();
                    byte[] payload = new byte[session.frameBuffer.remaining()];
                    session.frameBuffer.get(payload);
                    // 表示当前的一次请求已经被读完了，等待下一次请求
                    session.resetFrameState();

                    byte[] responsePayload = processRequest(payload); // 模拟执行方法
                    if (responsePayload != null) {
                        session.enqueueResponse(responsePayload);
                        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE); // 注册当前 channel 的写事件
                    }
                }

                if (buffer.remaining() == 0) {// 这一轮 Socket 发来的原始数据已经全被读完，退出循环
                    break;
                }
            } else {
                break;
            }
        }
        buffer.compact();
    }

    private void handleWrite(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ClientSession session = getOrCreateSession(key);
        // 写队列中有东西，需要写入数据
        while (!session.pendingWrites.isEmpty()) {
            // 读取队列头的第一个包
            ByteBuffer buffer = session.pendingWrites.peek();
            // 尝试写入 channel，从 position 读取直到 limit 为止，每次写入 position 往后移动一个字节
            channel.write(buffer);
            // 背压操作，因为当前 client 还没有读取完所有的数据，Server 发送太快，TCP 流量控制导致，当前发送缓冲区也满了
            // 等待发送缓冲区重新有空位再触发写事件
            if (buffer.hasRemaining()) {
                break;
            }
            session.pendingWrites.poll();
        }
        // 当前完全写完
        if (session.pendingWrites.isEmpty()) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE); // 取消 写事件 监听
        }
    }

    private byte[] processRequest(byte[] payload) {
        String request = new String(payload, StandardCharsets.UTF_8);
        System.out.println("Receive request: " + request);

        String responseText = "server already execute " + request;
        return responseText.getBytes(StandardCharsets.UTF_8);
    }

    private void ensureReadBufferCapacity(ClientSession session) {
        if (!session.readBuffer.hasRemaining()) {
            ByteBuffer newBuffer = ByteBuffer.allocate(session.readBuffer.capacity() * 2);
            session.readBuffer.flip();
            newBuffer.put(session.readBuffer);
            session.readBuffer = newBuffer;
        }
    }

    private ClientSession getOrCreateSession(SelectionKey key) {
        ClientSession session = (ClientSession) key.attachment();
        if (session == null) {
            session = new ClientSession();
            key.attach(session);
        }
        return session;
    }

    private void closeKey(SelectionKey key) {
        Channel channel = key.channel(); // 告诉 Selector 不再监听这个 key 以及 Channel
        try {
            key.cancel();
        } catch (Exception ignore) {
            // ignore
        }
        closeQuietly(channel); // 关闭这个底层的 SocketChannel ，也就是 TCP 连接
    }

    private void cleanup() {
        if (selector != null) {
            try {
                for (SelectionKey key : selector.keys()) {
                    closeKey(key); // 不再监听这个 key 对应的 channel
                }
            } catch (ClosedSelectorException ignore) {
                // selector already closed
            }
        }
        closeQuietly(serverChannel);// 关闭 TCP 连接
        closeQuietly(selector);
        serverChannel = null;
        selector = null;
        selectorThread = null;
        running.set(false);
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignore) {
                // ignore
            }
        }
    }

    @Override
    public synchronized void stop() {
        if (!running.get()) {
            return;
        }
        running.set(false);
        if (selector != null) {
            selector.wakeup();
        }

        Thread thread = selectorThread;
        if (thread != null && thread != Thread.currentThread()) {
            try {
                thread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static final class ClientSession {
        // 为了解决黏包、半包问题设置的 ReadBuffer
        private ByteBuffer readBuffer = ByteBuffer.allocate(8 * 1024);
        // 表示正在等待 4 字节的帧头
        private int expectedFrameSize = -1;
        // 真正读取数据的 Buffer
        private ByteBuffer frameBuffer;
        // 待写出的 ByteBuffer
        private final Deque<ByteBuffer> pendingWrites = new ArrayDeque<>();

        private void enqueueResponse(byte[] payload) {
            Objects.requireNonNull(payload, "payload");
            ByteBuffer frame = ByteBuffer.allocate(Integer.BYTES + payload.length);
            frame.putInt(payload.length);
            frame.put(payload);
            frame.flip();
            pendingWrites.offer(frame);
        }

        private void resetFrameState() {
            expectedFrameSize = -1;
            frameBuffer = null;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        RpcServer server = new NIOServer();
        server.start(9999);
        new CountDownLatch(1).await();
    }
}
