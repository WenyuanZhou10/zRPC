package cn.wenyuan.zrpc.network.handler;


import cn.wenyuan.zrpc.common.Message.RpcRequest;
import cn.wenyuan.zrpc.common.Message.RpcResponse;
import cn.wenyuan.zrpc.core.HeartbeatRequest;
import cn.wenyuan.zrpc.core.HeartbeatResponse;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
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
        if(evt instanceof IdleStateEvent e){
            if(e.state() == IdleState.WRITER_IDLE){
                // 客户端逻辑
                // 写空闲时，发送 ping
                log.debug("Writer idle, sending heartbeat ping to {}...",
                          ctx.channel().remoteAddress());
                // 发送 Ping 包
                ctx.writeAndFlush(new HeartbeatRequest());
            } else if (e.state() == IdleState.READER_IDLE){
                // 服务端逻辑
                // 读空闲，关闭连接
                log.warn("Reader idle for 30s, closing connection to {}",
                         ctx.channel().remoteAddress());
                // 关闭连接
                ctx.channel().close();
            } else {
                // 当前不是空闲时间所触发
                super.userEventTriggered(ctx, evt);
            }
        }
    }

    /**
     * 当读到新消息时被调用
     */
    @Override
    public void channelRead(
        ChannelHandlerContext ctx,
        Object msg
    ) throws Exception {
        if(msg instanceof HeartbeatRequest){
            // 服务端
            log.debug("Received heartbeat ping from {}", ctx.channel().remoteAddress());
            // 回复 pong
            ctx.writeAndFlush(new HeartbeatRequest());
            ReferenceCountUtil.release(msg);
        } else if (msg instanceof HeartbeatResponse){
            // 客户端收到 pong
            log.debug("Received heartbeat pong from {}", ctx.channel().remoteAddress());
            ReferenceCountUtil.release(msg);
        } else {
            ctx.fireChannelRead(msg);
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("HeartbeatHandler 捕获异常: {}", cause.getMessage(), cause);
        // 发生异常，关闭连接
        ctx.channel().close();
    }
}
