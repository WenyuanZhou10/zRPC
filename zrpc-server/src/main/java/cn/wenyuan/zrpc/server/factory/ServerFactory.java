package cn.wenyuan.zrpc.server.factory;

import cn.wenyuan.zrpc.core.registry.impl.LocalServiceCache;
import cn.wenyuan.zrpc.core.server.RpcServer;
import cn.wenyuan.zrpc.core.server.ServerOptions;
import cn.wenyuan.zrpc.transport.netty.server.NettyServer;

/**
 * @ClassName ServerFactory
 * @Description TODO
 * @Author wenyuan.zhou
 * @Date 2025/11/13 11:24
 * @Version 1.0
 */

public class ServerFactory {
    public static RpcServer createServer(LocalServiceCache serviceCache, ServerOptions options){
        return new NettyServer(serviceCache, options);
    }
}
