package cn.wenyuan.zrpc.Server.Netty.config;

import io.netty.util.concurrent.EventExecutorGroup;

/**
 * 服务器端启动配置。当前主要用于让调用方自定义执行 RpcServerHandler 的业务线程池，
 * 避免耗时业务逻辑阻塞 Netty 的 I/O 线程。
 */
public final class ServerOptions {

    private final EventExecutorGroup businessExecutorGroup;
    private final boolean manageBusinessExecutorLifecycle;

    private ServerOptions(
        EventExecutorGroup businessExecutorGroup,
        boolean manageBusinessExecutorLifecycle
    ) {
        this.businessExecutorGroup = businessExecutorGroup;
        this.manageBusinessExecutorLifecycle = manageBusinessExecutorLifecycle;
    }

    public EventExecutorGroup getBusinessExecutorGroup() {
        return businessExecutorGroup;
    }

    /**
     * 指示 NettyServer.stop() 是否需要主动调用业务线程池的 shutdownGracefully()。
     */
    public boolean isManageBusinessExecutorLifecycle() {
        return manageBusinessExecutorLifecycle;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ServerOptions defaults() {
        return builder().build();
    }

    public static final class Builder {
        private EventExecutorGroup businessExecutorGroup;
        private Boolean manageBusinessExecutorLifecycle;

        /**
         * 设置一个自定义线程池，用于执行 RpcServerHandler 的回调逻辑。
         */
        public Builder businessExecutorGroup(EventExecutorGroup businessExecutorGroup) {
            this.businessExecutorGroup = businessExecutorGroup;
            return this;
        }

        /**
         * 控制 NettyServer.stop() 是否应关闭该业务线程池。默认行为：
         * 调用方未提供自定义线程池：服务器自行创建，并负责关闭。
         * 调用方提供自定义线程池：服务器只使用，不会自动关闭，除非显式设置 true。
         * 如果不通过框架来自动关闭，则需要手动在ShutdownHook中关闭
         */
        public Builder manageBusinessExecutorLifecycle(boolean manageBusinessExecutorLifecycle) {
            this.manageBusinessExecutorLifecycle = manageBusinessExecutorLifecycle;
            return this;
        }

        public ServerOptions build() {
            boolean manageLifecycle = manageBusinessExecutorLifecycle != null
                    ? manageBusinessExecutorLifecycle
                    : businessExecutorGroup == null;
            return new ServerOptions(businessExecutorGroup, manageLifecycle);
        }
    }
}
