package cn.wenyuan.zrpc.registry.loadbalance;


import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.common.StringUtils;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName LoadBanlancerFactory
 * @Description TODO
 * @Author LoadBanlancerFactory
 * @Date 2025/11/1 18:12
 * @Version 1.0
 */

@Slf4j
public class LoadBanlancerFactory {

    private static final Map<String, LoadBalancer> BALANCER_CACHE = new ConcurrentHashMap<>();

    private static final LoadBalancer DEFAULT_BALANCER;

    static{
        log.info("开始加载 LoadBalancer SPI...");
        ServiceLoader<LoadBalancer> loader = ServiceLoader.load(LoadBalancer.class);

        for(LoadBalancer loadBalancer : loader){
            String name = loadBalancer.getName();
            if(!StringUtils.isEmpty(name)){
                BALANCER_CACHE.put(name, loadBalancer);
                log.info("成功加载负载均衡策略: name={}, class={}",
                         name, loadBalancer.getClass().getName());
            }
        }

        DEFAULT_BALANCER = BALANCER_CACHE.getOrDefault("roundrobin", loader.findFirst().orElse(null));
        if (DEFAULT_BALANCER != null) {
            log.info("默认负载均衡策略设置为: {}", DEFAULT_BALANCER.getName());
        } else {
            log.error("未找到任何负载均衡策略！");
        }
    }

    public static LoadBalancer get(String name){
        if(StringUtils.isEmpty(name)){
            return DEFAULT_BALANCER;
        }

        LoadBalancer loadBalancer = BALANCER_CACHE.get(name.toLowerCase());
        if (loadBalancer == null) {
            log.warn("未找到名为 [{}] 的负载均衡策略，将使用默认策略: {}",
                     name, DEFAULT_BALANCER.getName());
            return DEFAULT_BALANCER;
        }
        return loadBalancer;
    }
}
