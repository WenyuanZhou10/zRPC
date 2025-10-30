package cn.wenyuan.zrpc.Server;

public interface RpcServer {
    void start(int port);
    void stop();
}
