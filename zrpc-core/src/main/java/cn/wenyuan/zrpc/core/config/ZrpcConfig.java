package cn.wenyuan.zrpc.core.config;


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 承载 application.yml 中 zrpc 节点的配置。
 */
public class ZrpcConfig {

    private SerializationConfig serialization = new SerializationConfig();
    private RegistryConfig registry = new RegistryConfig();
    private ServerConfig server = new ServerConfig();
    private ClientConfig client = new ClientConfig();
    private LoadBalanceConfig loadBalance = new LoadBalanceConfig();

    public SerializationConfig getSerialization() {
        return serialization;
    }

    public void setSerialization(SerializationConfig serialization) {
        this.serialization = serialization;
    }

    public RegistryConfig getRegistry() {
        return registry;
    }

    public void setRegistry(RegistryConfig registry) {
        this.registry = registry;
    }

    public ServerConfig getServer() {
        return server;
    }

    public void setServer(ServerConfig server) {
        this.server = server;
    }

    public ClientConfig getClient() {
        return client;
    }

    public void setClient(ClientConfig client) {
        this.client = client;
    }

    public LoadBalanceConfig getLoadBalance() {
        return loadBalance;
    }

    public void setLoadBalance(LoadBalanceConfig loadBalance) {
        this.loadBalance = loadBalance;
    }

    public static class SerializationConfig {
        @JsonProperty("default-type")
        private String defaultType = "kryo";

        @JsonProperty("default-code")
        private Byte defaultCode;

        public String getDefaultType() {
            return defaultType;
        }

        public void setDefaultType(String defaultType) {
            this.defaultType = defaultType;
        }

        public Byte getDefaultCode() {
            return defaultCode;
        }

        public void setDefaultCode(Byte defaultCode) {
            this.defaultCode = defaultCode;
        }
    }

    public static class RegistryConfig {
        private String type = "zookeeper";
        private String address = "127.0.0.1:21888";

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }
    }

    public static class ServerConfig {
        private String host = "127.0.0.1";
        private int port = 9999;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public static class ClientConfig {
        @JsonProperty("request-timeout-millis")
        private int requestTimeoutMillis = 3000;

        public int getRequestTimeoutMillis() {
            return requestTimeoutMillis;
        }

        public void setRequestTimeoutMillis(int requestTimeoutMillis) {
            this.requestTimeoutMillis = requestTimeoutMillis;
        }
    }

    public static class LoadBalanceConfig {
        private String strategy = "roundrobin";

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }
    }
}
