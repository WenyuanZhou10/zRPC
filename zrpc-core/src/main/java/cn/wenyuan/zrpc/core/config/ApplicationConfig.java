package cn.wenyuan.zrpc.core.config;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;

/**
 * 负责从 classpath:application.yml 中加载配置。
 * 如果找不到配置文件或出现解析异常，将退回到内置默认值。
 */
@Slf4j
public final class ApplicationConfig {

    private static final String DEFAULT_CONFIG = "application.yml";
    private static final String CONFIG_LOCATION_PROPERTY = "zrpc.config";

    private static final ZrpcConfig CONFIG = load();

    private ApplicationConfig() {
    }

    public static ZrpcConfig getConfig() {
        return CONFIG;
    }

    private static ZrpcConfig load() {
        String location = System.getProperty(CONFIG_LOCATION_PROPERTY, DEFAULT_CONFIG);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ApplicationConfig.class.getClassLoader();
        }
        try (InputStream inputStream = classLoader.getResourceAsStream(location)) {
            if (inputStream == null) {
                log.warn("未在 classpath 下找到配置文件 {}，使用默认配置。", location);
                return new ZrpcConfig();
            }
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            JsonNode root = mapper.readTree(inputStream);
            if (root == null) {
                log.warn("配置文件 {} 为空，使用默认配置。", location);
                return new ZrpcConfig();
            }
            JsonNode zrpcNode = root.path("zrpc");
            if (zrpcNode.isMissingNode() || zrpcNode.isNull()) {
                log.warn("配置文件 {} 中未找到 zrpc 节点，使用默认配置。", location);
                return new ZrpcConfig();
            }
            ZrpcConfig config = mapper.treeToValue(zrpcNode, ZrpcConfig.class);
            return config != null ? config : new ZrpcConfig();
        } catch (Exception ex) {
            log.error("加载配置文件 {} 失败，将使用默认配置。", location, ex);
            return new ZrpcConfig();
        }
    }
}
