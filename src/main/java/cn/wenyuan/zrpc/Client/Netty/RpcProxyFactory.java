package cn.wenyuan.zrpc.Client.Netty;


import cn.wenyuan.zrpc.Client.RpcClient;
import cn.wenyuan.zrpc.Client.ServiceDiscovery.ServiceDiscovery;

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

    private final ServiceDiscovery serviceDiscovery;

    public RpcProxyFactory(ServiceDiscovery serviceDiscovery) {this.serviceDiscovery = serviceDiscovery;}

    /**
     * 实现通过服务发现机制来获取服务代理，无需指定host和port
     */
    public <T>T getProxy(Class<T> clazz){
        RpcInvocationHandler handler = new RpcInvocationHandler(clazz, serviceDiscovery, this);
        return (T)Proxy.newProxyInstance(clazz.getClassLoader(),
                                          new Class<?>[]{clazz},
                                          handler);
    }

    public RpcClient getOrCreateClient(String host, int port){
        String key = host + ":" + port;
        return clientCache.computeIfAbsent(key, k -> {
            RpcClient client = new NettyClient(host, port);
            client.connect();
            return client;
        });
    }


    /**
     * 当前逻辑是根据 接口类型、host、port 构建一个代理，并进行缓存，后续有相同的服务调用，无需重复构建
     */

//
//    public <T>T getProxy(Class<T> clazz, String host, int port){
//        RpcClient client = getOrCreateClient(host, port);
//        RpcInvocationHandler handler = new RpcInvocationHandler(client);
//        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, handler);
//    }
}
