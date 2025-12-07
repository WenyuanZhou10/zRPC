package cn.wenyuan.zrpc.common.exception;


/**
 * 标识可预期的 RPC 框架内部异常（例如限流、舱壁拒绝等）。
 */
public class RpcException extends RuntimeException {

    public RpcException(String message) {
        super(message);
    }

    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        // 可预期的异常无需昂贵的堆栈信息，避免日志噪声。
        return this;
    }
}
