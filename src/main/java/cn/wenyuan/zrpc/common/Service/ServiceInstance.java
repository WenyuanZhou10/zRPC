package cn.wenyuan.zrpc.common.Service;


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
}
