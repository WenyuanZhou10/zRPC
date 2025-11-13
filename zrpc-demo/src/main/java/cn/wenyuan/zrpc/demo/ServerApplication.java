package cn.wenyuan.zrpc.demo;

import cn.wenyuan.zrpc.server.factory.ServerFactory;
import cn.wenyuan.zrpc.core.server.ServerOptions;
import cn.wenyuan.zrpc.core.server.RpcServer;
import cn.wenyuan.zrpc.core.registry.impl.LocalServiceCache;
import cn.wenyuan.zrpc.demo.api.GreetingService;
import cn.wenyuan.zrpc.demo.api.impl.GreetingServiceImpl;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;

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

        System.out.println("启动 zRPC Netty Server，端口：" + port);
        server.start(port);
    }
}
