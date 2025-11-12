package cn.wenyuan.zrpc.Server.ServiceRegister.impl;

import cn.wenyuan.zrpc.Server.ServiceRegister.ServiceRegistry;
import cn.wenyuan.zrpc.common.Service.ServiceInstance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class commonServiceRegistry implements ServiceRegistry {

    private final Map<String, Object> serviceMap = new ConcurrentHashMap<>();


    @Override
    public void register(ServiceInstance instance) throws Exception {

    }

    @Override
    public void unregister(ServiceInstance instance) throws Exception {

    }

    @Override
    public void close() {

    }
}
