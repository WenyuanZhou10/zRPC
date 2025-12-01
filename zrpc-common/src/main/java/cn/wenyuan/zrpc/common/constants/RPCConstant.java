package cn.wenyuan.zrpc.common.constants;

/**
 * @ClassName RPCConstant
 * @Description TODO
 * @Author wenyuan.zhou
 * @Date 2025/11/13 17:57
 * @Version 1.0
 */

public final class RPCConstant {

    private RPCConstant() {
    }

    /**
     * JVM 参数：-Dzrpc.config=xxx 用于覆盖默认配置文件。
     */
    public static final String CONFIG_LOCATION_PROPERTY = "zrpc.config";

    /**
     * 首选配置文件名，放在业务模块（如 demo）的 resources 目录下即可被加载。
     */
    public static final String DEFAULT_CONFIG_FILE_NAME = "application.yml";
}
