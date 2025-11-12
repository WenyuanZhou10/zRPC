package cn.wenyuan.zrpc.common.Context;


import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * @ClassName RpcContext
 * @Description RPC 调用的“线程本地”上下文 (用于在客户端（调用前）设置隐式参数，并在服务端（调用中）读取。)
 * @Author RpcContext
 * @Date 2025/11/8 14:39
 * @Version 1.0
 */

@Slf4j
public class RpcContext {
    //
    private static final ThreadLocal<Map<String, String>> REQUEST_ATTACHMENTS =
        ThreadLocal.withInitial(ConcurrentHashMap::new);

    private static final ThreadLocal<Map<String, String>> RESPONSE_ATTACHMENTS =
        ThreadLocal.withInitial(ConcurrentHashMap::new);

    private static final ThreadLocal<Boolean> ASYNC_INVOKE_MODE =
        ThreadLocal.withInitial(() -> Boolean.FALSE);

    private static final ThreadLocal<CompletableFuture<?>> ASYNC_FUTURE = new ThreadLocal<>();


    public static void setAttachment(String key, String value) {
        if (value == null) {
            REQUEST_ATTACHMENTS.get().remove(key);
        } else {
            REQUEST_ATTACHMENTS.get().put(key, value);
        }
    }

    public static String getAttachment(String key) {
        return REQUEST_ATTACHMENTS.get().get(key);
    }

    /**
     * 便捷方法：将一次 RPC 调用声明成“异步模式”，并返回原始调用对应的 Future。
     * <p>使用方式示例：
     * <pre>
     * CompletableFuture<String> future = RpcContext.asyncCall(() -> greetingService.greet(user));
     * </pre>
     */
    public static <T> CompletableFuture<T> asyncCall(Supplier<T> invocation) {
        startAsyncCall();
        try {
            invocation.get();
        } catch (Throwable ex) {
            clearAsyncState();
            CompletableFuture<T> failed = new CompletableFuture<>();
            failed.completeExceptionally(ex);
            return failed;
        }
        CompletableFuture<T> future = getAsyncFuture();
        if (future == null) {
            CompletableFuture<T> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalStateException("异步调用未能生成 Future，请确认客户端代理是否已开启 async 模式"));
            return failed;
        }
        return future;
    }

    // --- 供【框架内部】调用的 API ---

    /**
     * 【框架用】获取所有待发送的参数（快照）
     */
    public static Map<String, String> getAttachments() {
        return new HashMap<>(REQUEST_ATTACHMENTS.get()); // 必须返回副本
    }

    /**
     * 【框架用】服务端在收到请求时，恢复上下文
     */
    public static void setAttachments(Map<String, String> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            REQUEST_ATTACHMENTS.get().clear();
        } else {
            REQUEST_ATTACHMENTS.get().putAll(attachments);
        }
    }

    /**
     * 【框架用】必须！清理 ThreadLocal，防止内存泄漏
     */
    public static void clear() {
        REQUEST_ATTACHMENTS.remove();
        RESPONSE_ATTACHMENTS.remove();
        clearAsyncState();
    }

    /* -------------------- Async Call state -------------------- */

    public static void startAsyncCall() {
        ASYNC_INVOKE_MODE.set(Boolean.TRUE);
        ASYNC_FUTURE.remove(); // 清掉上一轮可能遗留的 CompletableFuture 引用，确保当前异步调用拿到的 Future 是新鲜的。
    }

    public static boolean isAsyncCall() {
        return Boolean.TRUE.equals(ASYNC_INVOKE_MODE.get());
    }

    public static void publishAsyncFuture(CompletableFuture<?> future) {
        ASYNC_FUTURE.set(future);
    }

    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> getAsyncFuture() {
        try {
            return (CompletableFuture<T>) ASYNC_FUTURE.get();
        } finally {
            clearAsyncState();
        }
    }

    private static void clearAsyncState() {
        ASYNC_INVOKE_MODE.remove();
        ASYNC_FUTURE.remove();
    }
}
