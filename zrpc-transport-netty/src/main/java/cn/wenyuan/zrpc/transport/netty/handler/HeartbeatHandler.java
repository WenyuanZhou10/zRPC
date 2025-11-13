package cn.wenyuan.zrpc.transport.netty.handler;


import cn.wenyuan.zrpc.common.message.HeartbeatRequest;
import cn.wenyuan.zrpc.common.message.HeartbeatResponse;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable //
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {

    public static final HeartbeatHandler INSTANCE = new HeartbeatHandler();

    private HeartbeatHandler() { } // 构造方法私有、单例


    /**
     * 当捕获到空闲事件时被调用
     */
    @Override
    public void userEventTriggered(
        ChannelHandlerContext ctx,
        Object evt
    ) throws Exception {
        if (evt instanceof IdleStateEvent e) {
            if (e.state() == IdleState.WRITER_IDLE) {
                // 客户端逻辑：写空闲时发送 ping
                log.info("Writer idle detected, sending heartbeat ping to {}",
                         ctx.channel().remoteAddress());
                ctx.writeAndFlush(new HeartbeatRequest())
                   .addListener(future -> {
                       if (!future.isSuccess()) {
                           log.warn("Failed to send heartbeat ping to {}",
                                    ctx.channel().remoteAddress(), future.cause());
                       }
                   });
            } else if (e.state() == IdleState.READER_IDLE) {
                // 服务端逻辑：读空闲时关闭连接
                log.warn("Reader idle detected, closing connection to {}",
                         ctx.channel().remoteAddress());
                ctx.channel().close();
            } else {
                super.userEventTriggered(ctx, evt);
            }
            return;
        }
        super.userEventTriggered(ctx, evt);
    }

    /**
     * 当读到新消息时被调用
     */
    @Override
    public void channelRead(
        ChannelHandlerContext ctx,
        Object msg
    ) throws Exception {
        if (msg instanceof HeartbeatRequest) {
            // 服务端
            log.info("Received heartbeat ping from {}", ctx.channel().remoteAddress());
            // 回复 pong
            ctx.writeAndFlush(new HeartbeatResponse());
            ReferenceCountUtil.release(msg);
            return;
        } else if (msg instanceof HeartbeatResponse) {
            // 客户端收到 pong
            log.info("Received heartbeat pong from {}", ctx.channel().remoteAddress());
            ReferenceCountUtil.release(msg);
            return;
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("HeartbeatHandler 捕获异常: {}", cause.getMessage(), cause);
        // 发生异常，关闭连接
        ctx.channel().close();
    }
}
