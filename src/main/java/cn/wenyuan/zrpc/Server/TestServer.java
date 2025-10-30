package cn.wenyuan.zrpc.Server;

import cn.wenyuan.zrpc.Server.Impl.BIOSingleThreadServer;

public class TestServer {
    public static void main(String[] args) {
        BIOSingleThreadServer BIOSingleThreadServer = new BIOSingleThreadServer();
        BIOSingleThreadServer.start(9999);
    }
}
