package cn.wenyuan.zrpc.Client;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Minimal NIO client that talks to {@link cn.wenyuan.zrpc.Server.Impl.NIOServer}.
 * It uses the same length-prefixed UTF-8 framing protocol as the server.
 */
public class NIOClient {

    private static final int CONNECT_TIMEOUT_MILLIS = 5_000;

    public static String send(String host, int port, String message) {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(message, "message");

        ByteBuffer outbound = encodeFrame(message);

        try (SocketChannel channel = SocketChannel.open()) {
            channel.configureBlocking(true);
            channel.connect(new InetSocketAddress(host, port));
            if (!channel.finishConnect()) {
                throw new IOException("connection to " + host + ":" + port + " not completed");
            }

            while (outbound.hasRemaining()) {
                channel.write(outbound);
            }

            int responseLength = readFrameLength(channel);
            byte[] responseBytes = readFully(channel, responseLength);
            return new String(responseBytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("nio client request failed", e);
        }
    }

    private static ByteBuffer encodeFrame(String message) {
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        ByteBuffer frame = ByteBuffer.allocate(Integer.BYTES + payload.length);
        frame.putInt(payload.length);
        frame.put(payload);
        frame.flip();
        return frame;
    }

    private static int readFrameLength(SocketChannel channel) throws IOException {
        ByteBuffer lengthBuffer = ByteBuffer.allocate(Integer.BYTES);
        readBuffer(channel, lengthBuffer);
        lengthBuffer.flip();
        int length = lengthBuffer.getInt();
        if (length < 0) {
            throw new IOException("invalid response length: " + length);
        }
        return length;
    }

    private static byte[] readFully(SocketChannel channel, int length) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(length);
        readBuffer(channel, buffer);
        buffer.flip();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        return data;
    }

    private static void readBuffer(SocketChannel channel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            int read = channel.read(buffer);
            if (read == -1) {
                throw new EOFException("connection closed while reading");
            }
        }
    }

    public static void main(String[] args) {
        String response = send("127.0.0.1", 9999, "testMethod");
        System.out.println("Receive response: " + response);
    }
}
