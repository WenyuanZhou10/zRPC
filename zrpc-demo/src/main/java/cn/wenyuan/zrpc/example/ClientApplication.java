package cn.wenyuan.zrpc.example;

import cn.wenyuan.zrpc.Client.Netty.RpcProxyFactory;
import cn.wenyuan.zrpc.Client.ServiceDiscovery.ServiceDiscovery;
import cn.wenyuan.zrpc.Server.ServiceRegister.impl.ZKServiceRegistry;
import cn.wenyuan.zrpc.common.Context.RpcContext;
import cn.wenyuan.zrpc.example.api.GreetingService;
import cn.wenyuan.zrpc.example.dto.User;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public final class ClientApplication {

    private ClientApplication() {
    }

    public static void main(String[] args) throws Exception {
        ServiceDiscovery serviceDiscovery = new ZKServiceRegistry("127.0.0.1:2182");
        RpcProxyFactory factory = new RpcProxyFactory(serviceDiscovery);
        GreetingService greetingService = factory.getProxy(GreetingService.class);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.warn("JVM 正在关闭，开始执行 RPC 优雅停机...");
            factory.shutdown();
            log.warn("RPC 优雅停机完成。");
        }));

        User user = new User(1L, "zRPC Demo");

        String greeting = greetingService.greet(user);
        int sum = greetingService.sum(7, 5);
        log.info("同步 greet 调用结果: {}", greeting);
        log.info("同步 sum 调用结果: {}", sum);

        CompletableFuture<String> asyncGreet = RpcContext.asyncCall(() -> greetingService.greet(user));
        asyncGreet.thenAccept(res -> log.info("异步 greet 调用结果: {}", res))
                  .exceptionally(ex -> {
                      log.error("异步 greet 失败: {}", ex.getMessage());
                      return null;
                  });

        CompletableFuture<Integer> asyncSum = RpcContext.asyncCall(() -> greetingService.sum(7, 5));
        asyncSum.thenAccept(res -> log.info("异步 sum 调用结果: {}", res))
                .exceptionally(ex -> {
                    log.error("异步 sum 失败: {}", ex.getMessage());
                    return null;
                });

        log.info("异步调用已发起，主线程继续执行其他逻辑...");
    }
}
