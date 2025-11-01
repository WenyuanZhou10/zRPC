package cn.wenyuan.zrpc.Server.Netty.handler;

import cn.wenyuan.zrpc.Server.ServiceRegister.ServiceRegistry;
import cn.wenyuan.zrpc.common.Message.RpcRequest;
import cn.wenyuan.zrpc.common.Message.RpcResponse;
import cn.wenyuan.zrpc.common.Service.LocalServiceCache;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Getter;

import java.lang.reflect.Method;

/**
 * @ClassName RpcServerHandler
 * @Description TODO
 * @Author wenyuan.zhou
 * @Date 2025/10/31 10:17
 * @Version 1.0
 */

public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    @Getter
    private final LocalServiceCache serviceCache;

    public RpcServerHandler(LocalServiceCache serviceCache) {
        this.serviceCache = serviceCache;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, RpcRequest request) throws Exception {
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

            Object result = method.invoke(service, request.getParams());
            response.setSuccess(true);
            response.setResult(result);
        } catch (Exception ex) {
            response.setSuccess(false);
            response.setErrorMessage(ex.getMessage());
            if (ex instanceof Exception exception) {
                response.setError(exception);
            } else {
                response.setError(new RuntimeException(ex));
            }
        }

        ctx.writeAndFlush(response);
    }
}
