package cn.wenyuan.zrpc.core.filter;

import cn.wenyuan.zrpc.core.filter.impl.DefaultFilterChain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

public class FilterManager {

    private final List<Filter> filterList;

    private static class Holder {
        static final FilterManager INSTANCE = new FilterManager();
    }

    public static FilterManager getInstance(){
        return Holder.INSTANCE;
    }

    private FilterManager(){
        ServiceLoader<Filter> loader = ServiceLoader.load(Filter.class);
        List<Filter> loadedFilter = new ArrayList<>();

        for (Filter filter : loader) {
            loadedFilter.add(filter);
        }

        this.filterList = Collections.unmodifiableList(loadedFilter);
    }

    public FilterChain buildChain() {
        return new DefaultFilterChain(this.filterList, null);
    }

    public FilterChain buildChain(DefaultFilterChain.Invoker invoker) {
        return new DefaultFilterChain(this.filterList, invoker);
    }
}
