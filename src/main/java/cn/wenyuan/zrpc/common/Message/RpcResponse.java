package cn.wenyuan.zrpc.common.Message;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
@Builder
public class RpcResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String requestId;
    private boolean success;
    private Object result;       // 成功时填充
    private String errorCode;
    private String errorMessage;
    private Map<String, String> attachments;

}
