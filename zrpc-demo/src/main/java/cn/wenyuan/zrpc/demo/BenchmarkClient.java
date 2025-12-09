package cn.wenyuan.zrpc.demo;

import cn.wenyuan.zrpc.client.proxy.RpcProxyFactory;
import cn.wenyuan.zrpc.common.context.RpcContext;
import cn.wenyuan.zrpc.core.client.RpcClient;
import cn.wenyuan.zrpc.core.config.ApplicationConfig;
import cn.wenyuan.zrpc.core.config.ZrpcConfig;
import cn.wenyuan.zrpc.core.registry.ServiceDiscovery;
import cn.wenyuan.zrpc.core.registry.impl.ZKServiceRegistry;
import cn.wenyuan.zrpc.demo.api.GreetingService;
import cn.wenyuan.zrpc.demo.dto.User;
import cn.wenyuan.zrpc.transport.netty.client.NettyClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class BenchmarkClient {

    public static void main(String[] args) throws Exception {
        // 1. 初始化客户端 (单例复用)
        ZrpcConfig config = ApplicationConfig.getConfig();
        String registryAddress = config.getRegistry().getAddress();
        ServiceDiscovery serviceDiscovery = new ZKServiceRegistry(registryAddress);
        RpcProxyFactory factory = new RpcProxyFactory(serviceDiscovery);
        GreetingService greetingService = factory.getProxy(GreetingService.class);

        // 2. 预热 (Warm Up)
        // 让 JVM 的 JIT 编译器介入，让 Netty 建立好连接池
        System.out.println("🔥 开始预热...");
        for (int i = 0; i < 500; i++) {
            try {
                greetingService.greet(new User(0L, "warmup"));
            } catch (Exception ignored) {}
        }
        System.out.println("✅ 预热完成，准备压测！");

        // ---------------- 配置区 ----------------
        int threadCount = 200;       // 并发线程数 (模拟 50 个用户)
        int maxPendingPerThread = 100;
        int timeSeconds = 60;       // 压测时长 (秒)
        // ---------------------------------------

        // 统计指标
        AtomicLong successCount = new AtomicLong(0);
        AtomicLong failCount = new AtomicLong(0);
        long endTime = System.currentTimeMillis() + (timeSeconds * 1000L);

        // 3. 启动压测线程
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1); // 统一发令枪

        for (int i = 0; i < threadCount; i++) {
            int finalI = i;
            executor.execute(() -> {
                try {
                    startLatch.await(); // 等待所有线程就绪
                    List<CompletableFuture<Void>> pending = new ArrayList<>(maxPendingPerThread);
                    while (System.currentTimeMillis() < endTime) {
                        if (pending.size() >= maxPendingPerThread) {
                            waitAny(pending);
                        }
                        pending.add(callAsync(greetingService, finalI, successCount, failCount));
                    }
                    waitAll(pending);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // 4. 启动监控线程 (每秒打印一次报告)
        Thread monitorThread = new Thread(() -> {
            long lastSuccess = 0;
            long startTime = System.currentTimeMillis();
            
            while (System.currentTimeMillis() < endTime + 1000) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) { break; }

                long currentSuccess = successCount.get();
                long currentFail = failCount.get();
                long qps = currentSuccess - lastSuccess;
                lastSuccess = currentSuccess;
                
                long elapsed = (System.currentTimeMillis() - startTime) / 1000;

                System.out.printf("[Time: %2ds] QPS: %-6d | Fail: %-4d | Total: %d%n",
                        elapsed, qps, currentFail, currentSuccess);
            }
        });
        monitorThread.start();

        // 5. 正式开跑
        startLatch.countDown();

        // 等待结束
        executor.shutdown();
        executor.awaitTermination(timeSeconds + 5, TimeUnit.SECONDS);
        monitorThread.join();
        System.out.println("🏁 压测结束！");
        System.out.printf("总成功: %d, 总失败: %d%n", successCount.get(), failCount.get());
        factory.shutdown();
        System.exit(0);
    }

    private static CompletableFuture<Void> callAsync(
        GreetingService service,
        int userIndex,
        AtomicLong successCount,
        AtomicLong failCount
    ) {
        return RpcContext.asyncCall(() -> {
                             try {
                                 return service.greet(new User((long) userIndex, "benchmark"));
                             } catch (InterruptedException e) {
                                 throw new RuntimeException(e);
                             }
                         })
            .thenAccept(res -> {
                if (res != null) {
                    successCount.incrementAndGet();
                } else {
                    failCount.incrementAndGet();
                }
            }).exceptionally(ex -> {
                failCount.incrementAndGet();
                return null;
            });
    }

    private static void waitAny(List<CompletableFuture<Void>> futures) {
        CompletableFuture.anyOf(futures.toArray(new CompletableFuture[0])).join();
        futures.removeIf(CompletableFuture::isDone);
    }

    private static void waitAll(List<CompletableFuture<Void>> futures) {
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        futures.clear();
    }
}
