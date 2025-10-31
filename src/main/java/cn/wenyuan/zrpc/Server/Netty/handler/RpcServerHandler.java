package cn.wenyuan.zrpc.Server.Netty.handler;

import cn.wenyuan.zrpc.Server.ServiceRegister.ServiceRegistry;
import cn.wenyuan.zrpc.common.Message.RpcRequest;
import cn.wenyuan.zrpc.common.Message.RpcResponse;
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
    private final ServiceRegistry serviceRegistry;

    public RpcServerHandler(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, RpcRequest request) throws Exception {
        RpcResponse response = RpcResponse.builder()
                .requestId(request.getRequestId())
                .build();

        Object service = serviceRegistry.getService(request.getService());
        if(service == null){
            throw new RuntimeException("未找到服务: " + request.getService());
        }
        Method method = service.getClass().getMethod(
                request.getMethodName(),
                request.getParamsType()
        );

        Object result = method.invoke(service, request.getParams());

        response.setResult(result);

        ctx.writeAndFlush(result);
    }
}
