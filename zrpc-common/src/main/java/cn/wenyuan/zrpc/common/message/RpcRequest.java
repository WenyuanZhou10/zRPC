package cn.wenyuan.zrpc.common.message;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RpcRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String requestId;
    private String service;      // e.g. com.foo.UserService
    private String methodName;       // e.g. getUser
    private Object[] params;           //参数列表
    private Class<?>[] paramsType;      //参数类型
    private Map<String, String> headers; // 放超时时间、认证信息、序列化方式等
    private Object payload;      // 或 byte[]，视序列化方案而定
    private int timeoutMillis;

    private transient Runnable postProcessor;
}
