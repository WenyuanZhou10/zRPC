package cn.wenyuan.zrpc.Server.Impl;

import cn.wenyuan.zrpc.Server.RpcServer;
import cn.wenyuan.zrpc.common.Message.RpcRequest;
import cn.wenyuan.zrpc.common.Message.RpcResponse;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 */
public class BIOSingleThreadServer implements RpcServer {

    @Override
    public void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server start on port " + port + "!");
            while (true) {
                Socket socket = serverSocket.accept();
                Thread worker = new Thread(() -> handleClient(socket), "zrpc-io-" + socket.getPort());
                worker.start();
            }
        } catch (IOException e) {
            throw new RuntimeException("io server start failed", e);
        }
    }

    private void handleClient(Socket socket) {
        try (Socket client = socket;
             ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(client.getInputStream())) {

            RpcRequest request = (RpcRequest) ois.readObject();
            System.out.println("Receive request: " + request);

            RpcResponse response = execute(request);
            oos.writeObject(response);
            oos.flush();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("handle client error: " + e.getMessage());
        }
    }

    private RpcResponse execute(RpcRequest request) {
        // TODO: Replace with real service lookup & invocation.
        return RpcResponse.builder()
                .requestId(request.getRequestId())
                .success(true)
                .result("server already execute " + request.getMethodName())
                .build();
    }

    @Override
    public void stop() {
        // TODO: implement proper shutdown when you introduce lifecycle management.
    }
}
