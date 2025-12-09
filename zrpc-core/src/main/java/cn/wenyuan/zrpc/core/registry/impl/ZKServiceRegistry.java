package cn.wenyuan.zrpc.core.registry.impl;


import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.service.ServiceInstance;
import cn.wenyuan.zrpc.core.config.ApplicationConfig;
import cn.wenyuan.zrpc.core.config.ZrpcConfig;
import cn.wenyuan.zrpc.core.gray.GrayReleaseSelector;
import cn.wenyuan.zrpc.core.loadbalance.LoadBalancer;
import cn.wenyuan.zrpc.core.loadbalance.LoadBanlancerFactory;
import cn.wenyuan.zrpc.core.registry.ServiceRegistry;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceCache;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

/**
 * @ClassName ZKServiceRegister
 * @Description ZooKeeper
 * @Author ZKServiceRegister
 * @Date 2025/11/1 00:12
 * @Version 1.0
 */
@Slf4j
public class ZKServiceRegistry implements ServiceRegistry, cn.wenyuan.zrpc.core.registry.ServiceDiscovery {

    // Curator 客户端
    private final CuratorFramework client;
    // Curator 提供的服务发现 API
    private final ServiceDiscovery<ServiceInstance> serviceDiscovery;
    // ZK 中的根路径
    private static final String ZK_BASE_PATH = "/zrpc/service";
    // 本地服务缓存；每个 serviceName 对应一个 Curator ServiceCache
    private final Map<String, ServiceCache<ServiceInstance>> serviceCaches = new ConcurrentHashMap<>();
    // 负载均衡策略
    private final LoadBalancer loadBalancer;

    public ZKServiceRegistry(String zkAddress) throws Exception {
        ZrpcConfig config = ApplicationConfig.getConfig();
        String connectString = zkAddress;
        if ((connectString == null || connectString.isBlank()) && config != null
            && config.getRegistry() != null) {
            connectString = config.getRegistry().getAddress();
        }
        if (connectString == null || connectString.isBlank()) {
            throw new IllegalArgumentException("ZK 地址不能为空");
        }
        this.client = CuratorFrameworkFactory.builder()
            .connectString(connectString)
            .retryPolicy(new ExponentialBackoffRetry(1000, 3))
            .build();
        this.client.start();
        JsonInstanceSerializer<ServiceInstance> serializer =
            new JsonInstanceSerializer<>(ServiceInstance.class);

        this.serviceDiscovery = ServiceDiscoveryBuilder.builder(ServiceInstance.class)
                                                       .client(client)
                                                       .basePath(ZK_BASE_PATH) // ZK 中的根节点
                                                       .serializer(serializer)
                                                       .build();
        this.serviceDiscovery.start();
        String strategy = config != null && config.getLoadBalance() != null
            ? config.getLoadBalance().getStrategy()
            : null;
        this.loadBalancer = LoadBanlancerFactory.get(strategy);
    }

    @Override
    public void register(ServiceInstance instance) throws Exception {
        org.apache.curator.x.discovery.ServiceInstance<ServiceInstance> curatorInstance =
            org.apache.curator.x.discovery.ServiceInstance.<ServiceInstance>builder()
                                            .name(instance.getServiceName()) // 服务名
                                            .address(instance.getHost())     // host
                                            .port(instance.getPort())        // port
                                            .payload(instance)               // [关键] 完整的实例信息作为 payload
                                            .build();
        serviceDiscovery.registerService(curatorInstance);
    }

    @Override
    public void unregister(ServiceInstance instance) throws Exception {
        org.apache.curator.x.discovery.ServiceInstance<ServiceInstance> curatorInstance =
            org.apache.curator.x.discovery.ServiceInstance.<ServiceInstance>builder()
                                                          .name(instance.getServiceName())
                                                          .address(instance.getHost())
                                                          .port(instance.getPort())
                                                          .payload(instance)
                                                          .build();
        serviceDiscovery.unregisterService(curatorInstance);
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) throws Exception {
        List<ServiceInstance> cached = getCachedInstances(serviceName);
        if (!cached.isEmpty()) {
            return cached;
        }
        return queryDirectly(serviceName);
    }


    // TODO:使用SPI机制自定义负载均衡策略
    @Override
    public ServiceInstance getInstance(String serviceName, RpcRequest request) throws Exception {
        List<ServiceInstance> instances = getInstances(serviceName);
        instances = GrayReleaseSelector.select(instances, request);
        ServiceInstance serviceInstance = loadBalancer.select(instances, request);
        return serviceInstance;
    }

    @Override
    public void close() {
        try {
            for (Map.Entry<String, ServiceCache<ServiceInstance>> entry : serviceCaches.entrySet()) {
                try {
                    entry.getValue().close();
                } catch (IOException e) {
                    log.warn("关闭 ServiceCache [{}] 失败", entry.getKey(), e);
                }
            }
            serviceCaches.clear();
            if (serviceDiscovery != null) {
                serviceDiscovery.close();
            }
            if (client != null) {
                client.close();
            }
        } catch (Exception e) {
            log.error("关闭 ZKServiceRegistry 失败", e);
        }
    }

    private List<ServiceInstance> getCachedInstances(String serviceName) throws Exception {
        ServiceCache<ServiceInstance> cache = serviceCaches.get(serviceName);
        if (cache == null) {
            cache = createServiceCache(serviceName);
        }
        return cache.getInstances().stream()
            .map(org.apache.curator.x.discovery.ServiceInstance::getPayload)
            .collect(Collectors.toList());
    }

    private List<ServiceInstance> queryDirectly(String serviceName) throws Exception {
        return serviceDiscovery.queryForInstances(serviceName).stream()
            .map(org.apache.curator.x.discovery.ServiceInstance::getPayload)
            .collect(Collectors.toList());
    }

    private ServiceCache<ServiceInstance> createServiceCache(String serviceName) throws Exception {
        synchronized (serviceCaches) {
            ServiceCache<ServiceInstance> existing = serviceCaches.get(serviceName);
            if (existing != null) {
                return existing;
            }
            ServiceCache<ServiceInstance> cache =
                serviceDiscovery.serviceCacheBuilder()
                    .name(serviceName)
                    .build();
            cache.start();
            serviceCaches.put(serviceName, cache);
            return cache;
        }
    }
}
