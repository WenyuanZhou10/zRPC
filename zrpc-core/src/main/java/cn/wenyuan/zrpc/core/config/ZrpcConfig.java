package cn.wenyuan.zrpc.core.config;


import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

/**
 * 承载 application.yml 中 zrpc 节点的配置。
 */
@Data
public class ZrpcConfig {

    private SerializationConfig serialization = new SerializationConfig();
    private RegistryConfig registry = new RegistryConfig();
    private ServerConfig server = new ServerConfig();
    private ClientConfig client = new ClientConfig();
    @JsonProperty("load-balance")
    private LoadBalanceConfig loadBalance = new LoadBalanceConfig();
    @JsonProperty("ratelimit")
    private RateLimitConfig rateLimit = new RateLimitConfig();
    private BulkheadConfig bulkhead = new BulkheadConfig();

    @Data
    public static class SerializationConfig {
        @JsonProperty("default-type")
        private String defaultType = "kryo";

        @JsonProperty("default-code")
        private Byte defaultCode;

    }

    @Data
    public static class RegistryConfig {
        private String type = "zookeeper";
        private String address = "127.0.0.1:21888";

    }

    @Data
    public static class ServerConfig {
        private String host = "127.0.0.1";
        private int port = 9999;

    }

    @Data
    public static class ClientConfig {
        @JsonProperty("request-timeout-millis")
        private int requestTimeoutMillis = 3000;

    }

    @Data
    public static class LoadBalanceConfig {
        private String strategy = "roundrobin";

    }

    @Data
    public static class RateLimitConfig {
        @JsonProperty("default_algorithm")
        private String defaultAlgorithm = "token_bucket";

        @JsonProperty("token_bucket")
        private TokenBucketConfig tokenBucket = new TokenBucketConfig();

        @Data
        public static class TokenBucketConfig {
            private Integer qps = 10;
            private Integer capacity = 10;
        }
    }

    @Data
    public static class BulkheadConfig {
        @JsonProperty("default")
        private BulkheadRule defaultRule = new BulkheadRule();

        private Map<String, Map<String, BulkheadRule>> services = new HashMap<>();
    }

    @Data
    public static class BulkheadRule {
        @JsonProperty("max_concurrent")
        private Integer maxConcurrent = 1;
    }
}
