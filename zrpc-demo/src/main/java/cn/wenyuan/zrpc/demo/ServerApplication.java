package cn.wenyuan.zrpc.demo;

import cn.wenyuan.zrpc.core.config.ApplicationConfig;
import cn.wenyuan.zrpc.core.config.ZrpcConfig;
import cn.wenyuan.zrpc.core.registry.impl.LocalServiceCache;
import cn.wenyuan.zrpc.core.server.RpcServer;
import cn.wenyuan.zrpc.core.server.ServerOptions;
import cn.wenyuan.zrpc.demo.api.GreetingService;
import cn.wenyuan.zrpc.demo.api.impl.GreetingServiceImpl;
import cn.wenyuan.zrpc.server.factory.ServerFactory;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;

public final class ServerApplication {

    private ServerApplication() {
    }

    public static void main(String[] args) throws Exception {
        ZrpcConfig config = ApplicationConfig.getConfig();
        String host = config.getServer().getHost();
        int port = config.getServer().getPort();
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignore) {
                System.out.println("无效端口，使用默认端口 " + port);
            }
        }

        LocalServiceCache localServiceCache = new LocalServiceCache(host, port);
        GreetingService greetingService = new GreetingServiceImpl();
        localServiceCache.registerService(greetingService);

        // 示例：自定义业务线程池，避免长时间业务逻辑阻塞 Netty I/O 线程
        EventExecutorGroup bizExecutorGroup = new DefaultEventExecutorGroup(
                Math.max(4, Runtime.getRuntime().availableProcessors()),
                new DefaultThreadFactory("zrpc-example-biz")
        );

        ServerOptions options = ServerOptions.builder()
                .businessExecutorGroup(bizExecutorGroup)
                .manageBusinessExecutorLifecycle(false) // 样例中手动关闭
                .build();

        RpcServer server = ServerFactory.createServer(localServiceCache, options);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            localServiceCache.unregisterAllServices();
            server.stop();
            bizExecutorGroup.shutdownGracefully(); // 手动关闭
        }));

        System.out.println("启动 zRPC Netty Server，地址：" + host + ":" + port);
        server.start(port);
    }
}
