package cn.wenyuan.zrpc.core.registry.impl;


import cn.wenyuan.zrpc.common.service.ServiceInstance;
import cn.wenyuan.zrpc.core.config.ApplicationConfig;
import cn.wenyuan.zrpc.core.config.ZrpcConfig;
import cn.wenyuan.zrpc.core.registry.ServiceRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

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
        ZrpcConfig config = ApplicationConfig.getConfig();
        String zkAddress = config != null && config.getRegistry() != null
            ? config.getRegistry().getAddress()
            : null;
        this.serviceRegistry = new ZKServiceRegistry(zkAddress);
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
