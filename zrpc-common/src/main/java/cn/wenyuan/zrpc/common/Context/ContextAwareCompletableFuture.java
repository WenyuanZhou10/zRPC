package cn.wenyuan.zrpc.common.Context;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @ClassName ContextAwareCompletableFuture
 * @Description 用于在异步回调中恢复 RpcContext，并把用户的取消操作传递到底层 Future
 * @Author ContextAwareCompletableFuture
 * @Date 2025/11/8 14:52
 * @Version 1.0
 */

public class ContextAwareCompletableFuture<T> extends CompletableFuture<T> {

    // 真正的 future
    private final CompletableFuture<T> delegate;

    // “快照”，即发起请求时 RpcContext 里的内容
    private final Map<String, String> contextSnapshot;

    public ContextAwareCompletableFuture(
        CompletableFuture<T> delegate,
        Map<String, String> contextSnapshot
    ) {
        this.delegate = delegate;
        if (contextSnapshot == null || contextSnapshot.isEmpty()) {
            this.contextSnapshot = Collections.emptyMap();
        } else {
            this.contextSnapshot = Collections.unmodifiableMap(new HashMap<>(contextSnapshot));
        }

        // 当底层的 future 完成时也应该完成
        this.delegate.whenComplete((res, ex) -> runWithContext(() -> {
            if (ex != null) {
                ContextAwareCompletableFuture.super.completeExceptionally(ex);
            } else {
                ContextAwareCompletableFuture.super.complete(res);
            }
        }));
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean cancelled = delegate.cancel(mayInterruptIfRunning);
        if (cancelled) {
            return true;
        }
        return super.cancel(mayInterruptIfRunning);
    }

    private void runWithContext(Runnable action) {
        Map<String, String> previous = RpcContext.getAttachments();
        RpcContext.clear();
        if (!contextSnapshot.isEmpty()) {
            RpcContext.setAttachments(contextSnapshot);
        }
        try {
            action.run();
        } finally {
            RpcContext.clear();
            if (!previous.isEmpty()) {
                RpcContext.setAttachments(previous);
            }
        }
    }
}
