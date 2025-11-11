package cn.wenyuan.zrpc.Server.Netty;

import cn.wenyuan.zrpc.Server.Netty.config.ServerOptions;
import cn.wenyuan.zrpc.Server.Netty.nettyInitializer.NettyServerInitializer;
import cn.wenyuan.zrpc.Server.RpcServer;
import cn.wenyuan.zrpc.common.Service.LocalServiceCache;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;

public class NettyServer implements RpcServer {


    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final ServerBootstrap bootstrap;

    private final LocalServiceCache serviceCache;
    private final EventExecutorGroup businessExecutorGroup;
    private final boolean manageBusinessExecutorLifecycle;

    public NettyServer(LocalServiceCache serviceCache) {
        this(serviceCache, ServerOptions.defaults());
    }

    public NettyServer(LocalServiceCache serviceCache, ServerOptions serverOptions){
        if (serviceCache == null) {
            throw new IllegalArgumentException("LocalServiceCache must not be null");
        }
        this.serviceCache = serviceCache;

        ServerOptions options = serverOptions == null ? ServerOptions.defaults() : serverOptions;
        EventExecutorGroup providedExecutor = options.getBusinessExecutorGroup();
        if (providedExecutor != null) {
            this.businessExecutorGroup = providedExecutor;
            this.manageBusinessExecutorLifecycle = options.isManageBusinessExecutorLifecycle();
        } else {
            int bizThreads = Math.max(2, Runtime.getRuntime().availableProcessors());
            this.businessExecutorGroup = new DefaultEventExecutorGroup(
                bizThreads,
                new DefaultThreadFactory("zrpc-server-biz")
            );
            this.manageBusinessExecutorLifecycle = true;
        }

        // 主 Reactor, 单线程只负责 accept 事件
        this.bossGroup = new NioEventLoopGroup(1);

        // 从 Reactor, 负责 read 和 write 的 IO，默认使用 2 * CPU 核心线程
        this.workerGroup = new NioEventLoopGroup();

        this.bootstrap = new ServerBootstrap();
        this.bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new NettyServerInitializer(this.serviceCache, this.businessExecutorGroup));
    }

    @Override
    public void start(int port) {
        try {
            ChannelFuture future = bootstrap.bind(port).sync();
            System.out.println("服务器启动在端口："+port);
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally{
            stop();
        }
    }

    @Override
    public void stop() {
        System.out.println("正在关闭 Netty 服务器...");
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        if (manageBusinessExecutorLifecycle && businessExecutorGroup != null) {
            businessExecutorGroup.shutdownGracefully();
        }
    }
}
