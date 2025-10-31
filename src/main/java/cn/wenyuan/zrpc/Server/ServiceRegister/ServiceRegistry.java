package cn.wenyuan.zrpc.Server.ServiceRegister;

public interface ServiceRegistry {
    void register(String serviceName, Object service);

    Object getService(String serviceName);
}
