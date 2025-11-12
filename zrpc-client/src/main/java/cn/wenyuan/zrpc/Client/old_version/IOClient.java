package cn.wenyuan.zrpc.Client.old_version;

import cn.wenyuan.zrpc.common.Message.RpcRequest;
import cn.wenyuan.zrpc.common.Message.RpcResponse;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class IOClient {

    public static RpcResponse sendRequest(String host, int port, RpcRequest rpcRequest){
        try {
            Socket socket =  new Socket(host, port);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            oos.writeObject(rpcRequest);
            oos.flush();

            RpcResponse response = (RpcResponse)ois.readObject();
            return response;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        RpcRequest testMethod = RpcRequest.builder().requestId("1").methodName("testMethod").build();
        RpcResponse response = IOClient.sendRequest("127.0.0.1", 9999, testMethod);
        System.out.println(response + ": " + response.getResult());
    }
}
