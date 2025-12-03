package cn.wenyuan.zrpc.core.filter.client;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 客户端过滤器管理器，负责 SPI 加载和链路构建。
 */
public final class ClientFilterManager {

    private final List<ClientFilter> filters;

    private ClientFilterManager() {
        ServiceLoader<ClientFilter> loader = ServiceLoader.load(ClientFilter.class);
        List<ClientFilter> loaded = new ArrayList<>();
        for (ClientFilter filter : loader) {
            loaded.add(filter);
        }
        this.filters = Collections.unmodifiableList(loaded);
    }

    private static class Holder {
        private static final ClientFilterManager INSTANCE = new ClientFilterManager();
    }

    public static ClientFilterManager getInstance() {
        return Holder.INSTANCE;
    }

    public ClientFilterChain buildChain(ClientFilterChain.Invocation invocation) {
        return new ClientFilterChain(filters, invocation);
    }
}
