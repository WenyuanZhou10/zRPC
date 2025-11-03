package cn.wenyuan.zrpc.core;

import java.io.Serializable;

public class HeartbeatRequest implements Serializable {
    // 我们可以让它为空，或者带个时间戳
    private final long timestamp = System.currentTimeMillis();

    // Kryo 需要一个无参构造函数
    public HeartbeatRequest() {
    }

    public long getTimestamp() {
        return timestamp;
    }
}
