package cn.wenyuan.zrpc.example;

import cn.wenyuan.zrpc.Client.Netty.RpcProxyFactory;
import cn.wenyuan.zrpc.Client.ServiceDiscovery.ServiceDiscovery;
import cn.wenyuan.zrpc.Server.ServiceRegister.impl.ZKServiceRegistry;
import cn.wenyuan.zrpc.example.api.GreetingService;
import cn.wenyuan.zrpc.example.dto.User;
import lombok.extern.slf4j.Slf4j;

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

        for(int i = 0; i < 10; i++){
            User user = new User(1L, "zRPC Demo");
            String greeting = greetingService.greet(user);
            int sum = greetingService.sum(7, 5);
            System.out.println("调用 greet 返回: " + greeting);
            System.out.println("调用 sum 返回: " + sum);
        }
    }
}
