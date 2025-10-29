package cn.wenyuan.com.common.Message;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
public class RpcRequest {
    private String requestId;
    private String service;      // e.g. com.foo.UserService
    private String method;       // e.g. getUser
    private Map<String, String> headers; // 放超时时间、认证信息、序列化方式等
    private Object payload;      // 或 byte[]，视序列化方案而定
    private int timeoutMillis;      //
    // getter/setter 略
}
