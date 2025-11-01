package cn.wenyuan.zrpc.common.Service;


import cn.wenyuan.zrpc.Server.ServiceRegister.ServiceRegistry;
import cn.wenyuan.zrpc.Server.ServiceRegister.impl.ZKServiceRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName LocalServiceCache
 * @Description TODO
 * @Author LocalServiceCache
 * @Date 2025/11/1 01:15
 * @Version 1.0
 */

public class LocalServiceCache {
    private Map<String,Object> serviceProvider;

    private int port;
    private String host;

    private ServiceRegistry serviceRegistry;

    public LocalServiceCache(String host, int port) throws Exception {
        this.host = host;
        this.port = port;
        this.serviceRegistry = new ZKServiceRegistry("127.0.0.1:2182");
        this.serviceProvider = new ConcurrentHashMap<>();
    }

    public void registerService(Object service) throws Exception {
        Class<?>[] interfaceName=service.getClass().getInterfaces();

        for(Class<?> clazz : interfaceName){
            serviceProvider.put(clazz.getName(), service);
            serviceRegistry.register(new ServiceInstance(clazz.getName(),host,port));
        }
    }

    public Object getService(String serviceName){
        return serviceProvider.get(serviceName);
    }
}
