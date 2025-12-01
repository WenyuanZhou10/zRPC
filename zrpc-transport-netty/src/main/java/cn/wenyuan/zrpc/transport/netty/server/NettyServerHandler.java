package cn.wenyuan.zrpc.transport.netty.server;

import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.message.RpcResponse;
import cn.wenyuan.zrpc.core.registry.impl.LocalServiceCache;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName RpcServerHandler
 * @Description TODO
 * @Author wenyuan.zhou
 * @Date 2025/10/31 10:17
 * @Version 1.0
 */
@Slf4j
public class NettyServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private static final ExecutorService BIZ_POOL = new ThreadPoolExecutor(
        200,
        200,
        60,
        TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(1000),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );

    @Getter
    private final LocalServiceCache serviceCache;

    public NettyServerHandler(LocalServiceCache serviceCache) {
        this.serviceCache = serviceCache;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, RpcRequest request) throws Exception {
        BIZ_POOL.execute(() -> {
            try {
                log.debug("业务线程 {} 开始处理请求: {}", Thread.currentThread().getName(), request.getRequestId());

                RpcResponse response = RpcResponse.builder()
                                                  .requestId(request.getRequestId())
                                                  .build();

                try {
                    Object service = serviceCache.getService(request.getService());
                    if (service == null) {
                        throw new IllegalStateException("未找到服务: " + request.getService());
                    }
                    Method method = service.getClass().getMethod(
                        request.getMethodName(),
                        request.getParamsType()
                    );
                    Map<String, String> attachment = request.getHeaders();
                    log.info("获取消息：{}, trace-id : {}", request, attachment.get("traceId"));
                    Object result = method.invoke(service, request.getParams());
                    response.setSuccess(true);
                    response.setResult(result);
                } catch (Exception ex) {
                    response.setSuccess(false);
                    response.setErrorMessage(ex.getMessage());
                    response.setError((ex));
                }

                ctx.writeAndFlush(response).addListener((ChannelFutureListener) future -> {
                    // 无论成功还是失败都释放信号量
                    if (request.getPostProcessor() != null) {
                        log.info(Thread.currentThread().getName() + "释放信号量");
                        request.getPostProcessor().run();
                    }

                    if (!future.isSuccess()) {
                        log.error("发送响应失败", future.cause());
                    }
                });
            } catch (Exception e) {
                log.error("任务提交或执行过程发生意外", e);
            }
        });
    }
}
