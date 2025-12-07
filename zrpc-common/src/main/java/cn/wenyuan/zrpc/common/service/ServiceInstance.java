package cn.wenyuan.zrpc.common.service;


import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName ServiceInstance
 * @Description TODO
 * @Author ServiceInstance
 * @Date 2025/11/1 00:16
 * @Version 1.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceInstance {
    private String serviceName;

    private String host;

    private int port;

    private Integer weight;

    private String version;

    private boolean gray;

    private Map<String, String> metadata;

    public ServiceInstance(
        String name,
        String host,
        int port
    ) {
        this.serviceName = name;
        this.host = host;
        this.port = port;
    }

    public String getInstanceKey(){
        return this.host + ":" + this.port;
    }
}
