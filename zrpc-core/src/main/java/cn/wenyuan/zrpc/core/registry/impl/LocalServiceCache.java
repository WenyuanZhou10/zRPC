package cn.wenyuan.zrpc.core.registry.impl;


import cn.wenyuan.zrpc.core.registry.ServiceRegistry;
import cn.wenyuan.zrpc.common.service.ServiceInstance;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName LocalServiceCache
 * @Description TODO
 * @Author LocalServiceCache
 * @Date 2025/11/1 01:15
 * @Version 1.0
 */
@Slf4j
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

    public void unregisterAllServices() {
        for(String serviceName : serviceProvider.keySet()){
            try {
                ServiceInstance instance = new ServiceInstance(serviceName, this.host, this.port);
                this.serviceRegistry.unregister(instance);
            } catch (Exception e) {
                log.error("反注册服务 {} 失败", serviceName, e);
            }
        }
        this.serviceProvider.clear();

        if(this.serviceRegistry != null){
            this.serviceRegistry.close();
        }
    }
}
