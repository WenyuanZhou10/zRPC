package cn.wenyuan.zrpc.Client;

import cn.wenyuan.zrpc.common.Message.RpcRequest;
import cn.wenyuan.zrpc.common.Message.RpcResponse;

public class TestClient {
    public static void main(String[] args) {
        RpcRequest testMethod = RpcRequest.builder().requestId("1").method("testMethod").build();
        RpcResponse response = IOClient.sendRequest("127.0.0.1", 9999, testMethod);
        System.out.println(response + ": " + response.getResult());
    }
}
