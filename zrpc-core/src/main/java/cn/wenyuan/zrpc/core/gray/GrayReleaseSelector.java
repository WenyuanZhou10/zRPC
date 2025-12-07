package cn.wenyuan.zrpc.core.gray;


import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.service.ServiceInstance;
import cn.wenyuan.zrpc.core.config.ApplicationConfig;
import cn.wenyuan.zrpc.core.config.ZrpcConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 根据灰度配置决定请求路由到灰度实例还是稳定实例。
 */
public final class GrayReleaseSelector {

    private GrayReleaseSelector() {
    }

    public static List<ServiceInstance> select(List<ServiceInstance> instances, RpcRequest request) {
        if (instances == null || instances.isEmpty()) {
            return instances;
        }
        ZrpcConfig config = ApplicationConfig.getConfig();
        ZrpcConfig.GrayReleaseConfig grayConfig =
            config != null ? config.getGrayRelease() : null;
        if (grayConfig == null || !grayConfig.isEnabled()) {
            return instances;
        }

        List<ServiceInstance> grayInstances = new ArrayList<>();
        List<ServiceInstance> stableInstances = new ArrayList<>();
        for (ServiceInstance instance : instances) {
            if (isGrayInstance(instance, grayConfig)) {
                grayInstances.add(instance);
            } else {
                stableInstances.add(instance);
            }
        }

        if (grayInstances.isEmpty()) {
            return instances;
        }

        boolean routeToGray = shouldRouteToGray(grayConfig, request);
        if (routeToGray) {
            return grayInstances;
        }
        if (!stableInstances.isEmpty()) {
            return stableInstances;
        }
        return grayInstances;
    }

    private static boolean isGrayInstance(ServiceInstance instance, ZrpcConfig.GrayReleaseConfig config) {
        if (instance == null) {
            return false;
        }
        if (instance.isGray()) {
            return true;
        }
        if (config.getTargetVersion() != null && instance.getVersion() != null) {
            return Objects.equals(config.getTargetVersion(), instance.getVersion());
        }
        return false;
    }

    private static boolean shouldRouteToGray(ZrpcConfig.GrayReleaseConfig config, RpcRequest request) {
        Map<String, String> headers = request.getHeaders();
        String forceHeader = config.getForceHeader();
        if (forceHeader != null && headers != null) {
            String forcedValue = headers.get(forceHeader);
            if (forcedValue != null && forcedValue.equalsIgnoreCase(config.getTargetVersion())) {
                return true;
            }
        }
        String identityHeader = config.getIdentityHeader();
        if (identityHeader != null && headers != null) {
            String identity = headers.get(identityHeader);
            if (identity != null && config.getWhitelist() != null
                && config.getWhitelist().contains(identity)) {
                return true;
            }
        }

        Integer percentage = config.getPercentage();
        if (percentage != null && percentage > 0) {
            String seed;
            if (identityHeader != null && headers != null && headers.get(identityHeader) != null) {
                seed = headers.get(identityHeader);
            } else {
                seed = request.getRequestId();
            }
            int hash = Math.abs(seed != null ? seed.hashCode() : ThreadLocalRandom.current().nextInt());
            return hash % 100 < Math.min(percentage, 100);
        }
        return false;
    }
}
