package cn.wenyuan.zrpc.Client.Netty;


import cn.wenyuan.zrpc.Client.RpcClient;
import cn.wenyuan.zrpc.Client.ServiceDiscovery.ServiceDiscovery;
import cn.wenyuan.zrpc.common.Message.RpcRequest;
import cn.wenyuan.zrpc.common.Message.RpcResponse;
import cn.wenyuan.zrpc.common.Service.ServiceInstance;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

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
        // 跳过 Object 的 toString, hashCode, equals 等方法
        if (Object.class.equals(method.getDeclaringClass())) {
            return method.invoke(this, args);
        }
        // 1. 获取服务名 (例如 "com.example.UserService")
        String serviceName = serviceInterface.getName();

        // 2. 服务发现
        // TODO:负载均衡，目前暂时默认访问 第一个实例
        List<ServiceInstance> instances = serviceDiscovery.getInstances(serviceName);

        if(instances == null){
            throw new RuntimeException("No provider available for service: " + serviceName);
        }

        ServiceInstance instance = instances.get(0);

        // 3.获取RPCClient
        RpcClient client = clientFactory.getOrCreateClient(instance.getHost(), instance.getPort());

        // 1.构建 request
        RpcRequest request = RpcRequest.builder()
            .requestId(UUID.randomUUID().toString())
            .service(method.getDeclaringClass().getName())
            .methodName(method.getName())
            .params(args)
            .paramsType(method.getParameterTypes())
            .build();

        RpcResponse response = client.sendRequest(request);
        if (!response.isSuccess()) {
            String message = response.getErrorMessage() != null
                ? response.getErrorMessage()
                : "remote invocation failed";
            throw new RuntimeException(message, response.getError());
        }

        return response.getResult();
    }
}
