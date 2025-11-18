package cn.wenyuan.zrpc.core.ratelimit;

/**
 * @ClassName RateLimitConstants
 * @Description TODO
 * @Author wenyuan.zhou
 * @Date 2025/11/17 10:35
 * @Version 1.0
 */

public final class RateLimitConstants {

    public static final String PREFIX = "zrpc.ratelimit";

    public static final String ALGORITHM_NAME_TOKEN_BUCKET = "token_bucket";

    public static final String TOKEN_BUCKET_PREFIX = PREFIX + "." + ALGORITHM_NAME_TOKEN_BUCKET;

    public static final String TOKEN_BUCKET_QPS_KEY = TOKEN_BUCKET_PREFIX + ".qps";

    public static final String TOKEN_BUCKET_CAPACITY_KEY = TOKEN_BUCKET_PREFIX + ".capacity";
}
