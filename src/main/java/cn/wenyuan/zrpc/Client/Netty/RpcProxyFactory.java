package cn.wenyuan.zrpc.Client.Netty;


import cn.wenyuan.zrpc.Client.RpcClient;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName RpcProxyFactory
 * @Description TODO
 * @Author RpcProxyFactory
 * @Date 2025/10/31 01:14
 * @Version 1.0
 */

public class RpcProxyFactory {
    // Key: "host:port", Value: 对应的 RpcClient 实例
    private final Map<String, RpcClient> clientCache = new ConcurrentHashMap<>();


    private RpcClient getOrCreateClient(String host, int port){
        String key = host + ":" + port;

        clientCache.computeIfAbsent(key, k -> {
            RpcClient client = new NettyClient(host, port);
            client.connect();
            return client;
        });
        return null;
    }

    public <T>T getProxy(Class<T> clazz, String host, int port){
        RpcClient client = getOrCreateClient(host, port);
        RpcInvocationHandler handler = new RpcInvocationHandler(client);
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, handler);
    }
}
