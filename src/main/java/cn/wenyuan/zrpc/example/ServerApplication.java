package cn.wenyuan.zrpc.example;

import cn.wenyuan.zrpc.Server.Netty.NettyServer;
import cn.wenyuan.zrpc.Server.RpcServer;
import cn.wenyuan.zrpc.common.Service.LocalServiceCache;
import cn.wenyuan.zrpc.example.api.GreetingService;
import cn.wenyuan.zrpc.example.api.impl.GreetingServiceImpl;

public final class ServerApplication {

    private ServerApplication() {
    }

    public static void main(String[] args) throws Exception {
        int port = 9999;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignore) {
                System.out.println("无效端口，使用默认端口 9999");
            }
        }

        LocalServiceCache localServiceCache = new LocalServiceCache("127.0.0.1", port);
        GreetingService greetingService = new GreetingServiceImpl();
        localServiceCache.registerService(greetingService);

        RpcServer server = new NettyServer(localServiceCache);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            localServiceCache.unregisterAllServices();
            server.stop();
        }));

        System.out.println("启动 zRPC Netty Server，端口：" + port);
        server.start(port);
    }
}
