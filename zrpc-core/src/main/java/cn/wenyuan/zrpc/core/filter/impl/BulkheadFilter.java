package cn.wenyuan.zrpc.core.filter.impl;


import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.message.RpcResponse;
import cn.wenyuan.zrpc.core.filter.Filter;
import cn.wenyuan.zrpc.core.filter.FilterChain;
import cn.wenyuan.zrpc.core.config.ApplicationConfig;
import cn.wenyuan.zrpc.core.config.ZrpcConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * @ClassName BulkheadFilter
 * @Description TODO
 * @Author BulkheadFilter
 * @Date 2025/12/1 01:30
 * @Version 1.0
 */
@Slf4j
public class BulkheadFilter implements Filter {
    // KEY: 服务名.方法名 Value: 信号量
    private final Map<String, Semaphore> semaphoreMap = new ConcurrentHashMap<>();

    private static final int DEFAULT_MAX_CONCURRENT = 1;

    @Override
    public void filter(
        RpcRequest request,
        RpcResponse response,
        FilterChain chain
    ) {
        String key = request.getService() + "." + request.getMethodName();
        Semaphore semaphore = semaphoreMap.computeIfAbsent(key, k -> {
            int max = resolveMaxConcurrent(request.getService(), request.getMethodName());

            log.info("【Bulkhead】为 " + k + " 创建舱壁，最大并发: " + max);
            return new Semaphore(max);
        });
        if (semaphore.tryAcquire()) {
            request.setPostProcessor(() -> {
                semaphore.release();
                log.debug("【Bulkhead】释放许可: {}", key);
            });
            // 4. 获取成功，放行
            try {
                chain.doFilter(request, response);
            } catch (Exception e) {
                log.error("发生错误，释放信号量");
                semaphore.release();
            }
        } else {
            // 6. 获取失败 (舱位满了)
            // 快速失败，保护线程池
            String msg = String.format("服务 [%s] 并发过高，已触发舱壁隔离。", key);
            log.error(msg);
            // 设置异常到响应中 (不要抛出，而是走正常响应流程返回错误)
            response.setError(new RuntimeException(msg));
            response.setSuccess(false);
            response.setErrorMessage(msg);
        }
    }

    private int resolveMaxConcurrent(String service, String method) {
        ZrpcConfig config = ApplicationConfig.getConfig();
        if (config != null && config.getBulkhead() != null) {
            ZrpcConfig.BulkheadConfig bulkheadConfig = config.getBulkhead();
            Map<String, Map<String, ZrpcConfig.BulkheadRule>> services = bulkheadConfig.getServices();
            if (services != null) {
                Map<String, ZrpcConfig.BulkheadRule> methods = services.get(service);
                if (methods != null) {
                    ZrpcConfig.BulkheadRule rule = methods.get(method);
                    if (rule != null && rule.getMaxConcurrent() != null && rule.getMaxConcurrent() > 0) {
                        return rule.getMaxConcurrent();
                    }
                }
            }
            ZrpcConfig.BulkheadRule defaultRule = bulkheadConfig.getDefaultRule();
            if (defaultRule != null && defaultRule.getMaxConcurrent() != null
                && defaultRule.getMaxConcurrent() > 0) {
                return defaultRule.getMaxConcurrent();
            }
        }
        return DEFAULT_MAX_CONCURRENT;
    }
}
