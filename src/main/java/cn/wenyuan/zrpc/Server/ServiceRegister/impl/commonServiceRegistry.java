package cn.wenyuan.zrpc.Server.ServiceRegister.impl;

import cn.wenyuan.zrpc.Server.ServiceRegister.ServiceRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class commonServiceRegistry implements ServiceRegistry {

    private final Map<String, Object> serviceMap = new ConcurrentHashMap<>();

    @Override
    public void register(String serviceName, Object service) {
        serviceMap.put(serviceName, service);
        System.out.println(serviceName + "服务已注册");
    }

    @Override
    public Object getService(String serviceName) {
        Object service = serviceMap.get(serviceName);
        if(service==null){
            System.out.println("服务未找到" + serviceName);
        }
        return service;
    }
}
