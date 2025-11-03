package cn.wenyuan.zrpc.core;

import java.io.Serializable;

public class HeartbeatResponse implements Serializable {
    private final long timestamp = System.currentTimeMillis();

    public HeartbeatResponse() {
    }
    
    public long getTimestamp() {
        return timestamp;
    }
}