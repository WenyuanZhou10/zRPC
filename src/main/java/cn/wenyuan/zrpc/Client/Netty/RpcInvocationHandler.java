package cn.wenyuan.zrpc.Client.Netty;


import cn.wenyuan.zrpc.Client.RpcClient;
import cn.wenyuan.zrpc.common.Message.RpcRequest;
import cn.wenyuan.zrpc.common.Message.RpcResponse;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @ClassName RpcInvocationHandler
 * @Description 专用的 RPC 调用处理器: 有一个特定的 RpcClient实例
 * @Author RpcInvocationHandler
 * @Date 2025/10/31 00:45
 * @Version 1.0
 */

public class RpcInvocationHandler implements InvocationHandler {

    private final RpcClient rpcClient;

    public RpcInvocationHandler(RpcClient rpcClient) {this.rpcClient = rpcClient;}

    @Override
    public Object invoke(
        Object proxy,
        Method method,
        Object[] args
    ) throws Throwable {
        // 跳过 Object 的 toString, hashCode, equals 等方法
        if (Object.class.equals(method.getDeclaringClass())) {
            return method.invoke(this, args);
        }

        // 1.构建 request
        RpcRequest request = RpcRequest.builder()
            .service(method.getDeclaringClass().getName())
            .methodName(method.getName())
            .params(args)
            .paramsType(method.getParameterTypes())
            .build();

        RpcResponse response = rpcClient.sendRequest(request);

        return response.getResult();
    }
}
