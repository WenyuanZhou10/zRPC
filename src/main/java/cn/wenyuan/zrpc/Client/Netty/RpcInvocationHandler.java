package cn.wenyuan.zrpc.Client.Netty;


import cn.wenyuan.zrpc.Client.RpcClient;
import cn.wenyuan.zrpc.Client.ServiceDiscovery.ServiceDiscovery;
import cn.wenyuan.zrpc.common.Message.RpcRequest;
import cn.wenyuan.zrpc.common.Message.RpcResponse;
import cn.wenyuan.zrpc.common.Service.ServiceInstance;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @ClassName RpcInvocationHandler
 * @Description 专用的 RPC 调用处理器: 有一个特定的 RpcClient实例
 * @Author RpcInvocationHandler
 * @Date 2025/10/31 00:45
 * @Version 1.0
 */

public class RpcInvocationHandler implements InvocationHandler {

    private final Class<?> serviceInterface;
    private final ServiceDiscovery serviceDiscovery;
    private final RpcProxyFactory clientFactory; // 用来获取 RpcClient 连接

    public RpcInvocationHandler(Class<?> serviceInterface,
                                ServiceDiscovery serviceDiscovery,
                                RpcProxyFactory clientFactory) {
        this.serviceInterface = serviceInterface;
        this.serviceDiscovery = serviceDiscovery;
        this.clientFactory = clientFactory;
    }

    @Override
    public Object invoke(
        Object proxy,
        Method method,
        Object[] args
    ) throws Throwable {
        // 构建 request
        RpcRequest request = RpcRequest.builder()
                                       .requestId(UUID.randomUUID().toString())
                                       .service(method.getDeclaringClass().getName())
                                       .methodName(method.getName())
                                       .params(args)
                                       .paramsType(method.getParameterTypes())
                                       .timeoutMillis(5000)
                                       .build();

        // 跳过 Object 的 toString, hashCode, equals 等方法
        if (Object.class.equals(method.getDeclaringClass())) {
            return method.invoke(this, args);
        }
        // 1. 获取服务名 (例如 "com.example.UserService")
        String serviceName = serviceInterface.getName();

        // 2. 服务发现
        ServiceInstance instance = serviceDiscovery.getInstance(serviceName, request);

        if(instance == null){
            throw new RuntimeException("No provider available for service: " + serviceName);
        }

        // 3.获取RPCClient
        RpcClient client = clientFactory.getOrCreateClient(instance.getHost(), instance.getPort());

        CompletableFuture<RpcResponse> future = client.sendRequest(request);
        RpcResponse response = future.get();

        return response.getResult();
    }
}
