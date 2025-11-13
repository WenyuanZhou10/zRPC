package cn.wenyuan.zrpc.core.server;

public interface RpcServer {
    void start(int port);
    void stop();
}
