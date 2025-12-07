package cn.wenyuan.zrpc.core.config;


import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    @JsonProperty("circuit-breaker")
    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();
    @JsonProperty("tracing")
    private TracingConfig tracing = new TracingConfig();
    @JsonProperty("retry")
    private RetryConfig retry = new RetryConfig();
    @JsonProperty("gray-release")
    private GrayReleaseConfig grayRelease = new GrayReleaseConfig();

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
        private String version = "stable";
        @JsonProperty("gray")
        private boolean gray = false;
        private Map<String, String> metadata = new HashMap<>();

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

    @Data
    public static class CircuitBreakerConfig {
        @JsonProperty("failure-rate-threshold")
        private Float failureRateThreshold = 50f;

        @JsonProperty("slow-call-rate-threshold")
        private Float slowCallRateThreshold = 100f;

        @JsonProperty("slow-call-duration-ms")
        private Long slowCallDurationMs = 2000L;

        @JsonProperty("wait-duration-ms")
        private Long waitDurationMs = 5000L;

        @JsonProperty("sliding-window-size")
        private Integer slidingWindowSize = 10;

        @JsonProperty("minimum-number-of-calls")
        private Integer minimumNumberOfCalls = 5;

        @JsonProperty("permitted-calls-in-half-open")
        private Integer permittedNumberOfCallsInHalfOpenState = 2;
    }

    @Data
    public static class TracingConfig {
        private boolean enabled = false;

        @JsonProperty("service-name")
        private String serviceName = "zrpc-app";

        @JsonProperty("zipkin-url")
        private String zipkinUrl = "http://localhost:9411/api/v2/spans";

        @JsonProperty("sampler-rate")
        private Double samplerRate = 1.0;
    }

    @Data
    public static class RetryConfig {
        private boolean enabled = false;

        @JsonProperty("max-attempts")
        private Integer maxAttempts = 2;

        @JsonProperty("initial-delay-ms")
        private Long initialDelayMillis = 50L;

        @JsonProperty("backoff-multiplier")
        private Double backoffMultiplier = 2.0;

        @JsonProperty("retry-on")
        private RetryCondition retryCondition = RetryCondition.NETWORK_ONLY;
    }

    public enum RetryCondition {
        NETWORK_ONLY,
        NETWORK_AND_SERVER_BUSY
    }

    @Data
    public static class GrayReleaseConfig {
        private boolean enabled = false;
        @JsonProperty("target-version")
        private String targetVersion = "gray";
        @JsonProperty("force-header")
        private String forceHeader = "gray-tag";
        @JsonProperty("identity-header")
        private String identityHeader = "user-id";
        @JsonProperty("whitelist")
        private List<String> whitelist = new ArrayList<>();
        @JsonProperty("percentage")
        private Integer percentage = 0;
    }
}
