package cn.wenyuan.zrpc.client.proxy;


import cn.wenyuan.zrpc.core.client.RpcClient;
import cn.wenyuan.zrpc.core.filter.client.ClientFilterChain;
import cn.wenyuan.zrpc.core.filter.client.ClientFilterManager;
import cn.wenyuan.zrpc.core.registry.ServiceDiscovery;
import cn.wenyuan.zrpc.common.context.RpcContext;
import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.message.RpcResponse;
import cn.wenyuan.zrpc.common.service.ServiceInstance;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @ClassName RpcInvocationHandler
 * @Description 专用的 RPC 调用处理器: 有一个特定的 RpcClient实例
 * @Author RpcInvocationHandler
 * @Date 2025/10/31 00:45
 * @Version 1.0
 */

@Slf4j
public class NewRpcInvocationHandler implements InvocationHandler {

    private static final String TRACE_ID_KEY = "traceId";
    private static final ClientFilterManager CLIENT_FILTER_MANAGER = ClientFilterManager.getInstance();

    private final Class<?> serviceInterface;
    private final ServiceDiscovery serviceDiscovery;
    private final RpcProxyFactory clientFactory; // 用来获取 RpcClient 连接

    public NewRpcInvocationHandler(Class<?> serviceInterface,
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
        String proxyInterfaceName = serviceInterface.getName();

        String serviceName;

        if (proxyInterfaceName.endsWith("Async")) {
            serviceName = proxyInterfaceName.substring(0, proxyInterfaceName.length() - 5);
        } else {
            serviceName = proxyInterfaceName;
        }

        Map<String, String> attachments = RpcContext.getAttachments();
        boolean traceIdGenerated = false;
        String traceId = attachments.get(TRACE_ID_KEY);
        if (traceId == null || traceId.isEmpty()) {
            traceIdGenerated = true;
            traceId = UUID.randomUUID().toString();
            RpcContext.setAttachment(TRACE_ID_KEY, traceId);
            attachments = RpcContext.getAttachments();
        }

        // 构建 request
        RpcRequest request = RpcRequest.builder()
                                       .requestId(UUID.randomUUID().toString())
                                       .service(serviceName)
                                       .methodName(method.getName())
                                       .params(args)
                                       .paramsType(method.getParameterTypes())
                                       .timeoutMillis(5000)
                                       .headers(attachments)
                                       .build();

        // 2. 服务发现
        ServiceInstance instance = serviceDiscovery.getInstance(serviceName, request);


        if(instance == null){
            throw new RuntimeException("No provider available for service: " + serviceName);
        }

        // 3.获取RPCClient
        RpcClient client = clientFactory.getOrCreateClient(instance.getHost(), instance.getPort());

        ClientFilterChain clientFilterChain = CLIENT_FILTER_MANAGER.buildChain(req -> client.sendRequest(req));
        // 网络层返回的响应结果
        CompletableFuture<RpcResponse> responseFuture = clientFilterChain.doFilter(request);
        // responseFuture 的 thenApply 得到的派生 Future
        CompletableFuture<Object> resultFuture = responseFuture.thenApply(rpcResponse -> {
            if (!rpcResponse.isSuccess()) {
                throw new RuntimeException(rpcResponse.getErrorMessage());
            }
            return rpcResponse.getResult();
        });

        Class<?> returnType = method.getReturnType();

        try {
            if (RpcContext.isAsyncCall()) { // 客户端开启异步，接口返回值不是 CompletableFuture
                log.debug("检测到线程处于 RpcContext 异步模式，返回原始 Future。");
                RpcContext.publishAsyncFuture(resultFuture);
                return defaultValue(returnType);
            }

            if(CompletableFuture.class.isAssignableFrom(returnType)){ // 接口返回的是 CompletableFuture 或其子类
                log.debug("返回 CompletableFuture 以供调用方自行处理。");
                return resultFuture;
            }

            log.debug("同步调用，将阻塞等待 RPC 响应。");
            return resultFuture.get();
        } finally {
            if (traceIdGenerated) {
                RpcContext.setAttachment(TRACE_ID_KEY, null);
            }
        }
    }

    private Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive() || void.class.equals(returnType)) {
            return null;
        }
        if (boolean.class.equals(returnType)) {
            return false;
        }
        if (char.class.equals(returnType)) {
            return '\u0000';
        }
        if (byte.class.equals(returnType)) {
            return (byte) 0;
        }
        if (short.class.equals(returnType)) {
            return (short) 0;
        }
        if (int.class.equals(returnType)) {
            return 0;
        }
        if (long.class.equals(returnType)) {
            return 0L;
        }
        if (float.class.equals(returnType)) {
            return 0f;
        }
        if (double.class.equals(returnType)) {
            return 0d;
        }
        return null;
    }
}
