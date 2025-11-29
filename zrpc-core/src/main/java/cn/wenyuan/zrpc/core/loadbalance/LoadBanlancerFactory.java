package cn.wenyuan.zrpc.core.loadbalance;


import cn.wenyuan.zrpc.core.config.ApplicationConfig;
import cn.wenyuan.zrpc.core.config.ZrpcConfig;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.common.StringUtils;

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
                BALANCER_CACHE.put(name.toLowerCase(Locale.ROOT), loadBalancer);
                log.info("成功加载负载均衡策略: name={}, class={}",
                         name, loadBalancer.getClass().getName());
            }
        }

        DEFAULT_BALANCER = determineDefaultBalancer();
    }

    public static LoadBalancer get(String name){
        LoadBalancer loadBalancer = getInternal(name);
        if(loadBalancer != null){
            return loadBalancer;
        }
        if (DEFAULT_BALANCER == null) {
            log.error("负载均衡器未初始化成功，无法提供默认实现。");
            return DEFAULT_BALANCER;
        }
        log.warn("未找到名为 [{}] 的负载均衡策略，将使用默认策略: {}",
                 name, DEFAULT_BALANCER.getName());
        return DEFAULT_BALANCER;
    }

    private static LoadBalancer determineDefaultBalancer() {
        ZrpcConfig config = ApplicationConfig.getConfig();
        LoadBalancer configured = getInternal(config != null && config.getLoadBalance() != null
                ? config.getLoadBalance().getStrategy()
                : null);
        if (configured != null) {
            log.info("默认负载均衡策略设置为配置项: {}", configured.getName());
            return configured;
        }
        LoadBalancer roundRobin = BALANCER_CACHE.get("roundrobin");
        if (roundRobin != null) {
            log.info("使用 roundrobin 作为默认负载均衡策略。");
            return roundRobin;
        }
        LoadBalancer fallback = BALANCER_CACHE.values().stream().findFirst().orElse(null);
        if (fallback != null) {
            log.warn("没有 roundrobin 策略，回退到 {} 作为默认值。", fallback.getName());
        } else {
            log.error("未找到任何负载均衡策略！");
        }
        return fallback;
    }

    private static LoadBalancer getInternal(String name) {
        if(StringUtils.isEmpty(name)){
            return null;
        }
        return BALANCER_CACHE.get(name.toLowerCase(Locale.ROOT));
    }
}
