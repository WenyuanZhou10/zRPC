package cn.wenyuan.zrpc.network.client;

import cn.wenyuan.zrpc.common.Message.RpcResponse;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @ClassName RpcTimeoutManager
 * @Description TODO
 * @Author wenyuan.zhou
 * @Date 2025/11/7 15:40
 * @Version 1.0
 */

public class RpcTimeoutManager {
    /**
     * 【核心】
     * 1. 这是一个单例、全局的 HashedWheelTimer
     * 2. tickDuration (100ms): 时间轮“指针”多久走一格
     * 3. ticksPerWheel (512): 时间轮一圈有多少格
     * (这意味着一圈是 100ms * 512 = 51.2 秒。足够用了)
     */
    private static final HashedWheelTimer TIMER = new HashedWheelTimer(
            new DefaultThreadFactory("rpc-timeout-janitor"),
            100, TimeUnit.SECONDS,
            512
    );

    public static void scheduleTimeout(String requestId, CompletableFuture<RpcResponse> future, long timeoutMillis){
        TimerTask timeoutTask = (Timeout timeout) -> {
            if(future.isDone()){
                return;
            }

            future.completeExceptionally(
                    new TimeoutException("Request timed out after " + timeoutMillis + "ms")
            );
        };

        TIMER.newTimeout(timeoutTask, timeoutMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 客户端关闭时必须被调用
     */
    public static void stop() {
        TIMER.stop();
    }
}
