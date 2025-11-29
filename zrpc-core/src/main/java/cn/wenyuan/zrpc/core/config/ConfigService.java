package cn.wenyuan.zrpc.core.config;

import cn.wenyuan.zrpc.common.constants.RPCConstant;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import io.netty.util.internal.StringUtil;
import org.apache.zookeeper.common.StringUtils;
import org.yaml.snakeyaml.Yaml;
/**
 * @ClassName ConfigService
 * @Description TODO
 * @Author wenyuan.zhou
 * @Date 2025/11/17 10:10
 * @Version 1.0
 */

public class ConfigService {

    private ConfigService() {}

    private static class ConfigHolder {
        private static final Map<String, Object> COFIG_MAP = loadConfig();

        private static Map<String, Object> loadConfig() {
            try (InputStream input = ConfigService.class.getClassLoader()
                    .getResourceAsStream(RPCConstant.CONFIG_FILE_NAME)) {

                if (input == null) {
                    return Collections.emptyMap();
                }

                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(input);

                if (data == null) {
                    return Collections.emptyMap();
                }

                return data;

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static Object getValue(String key){
        if(StringUtils.isEmpty(key)){
            return null;
        }

        Map<String, Object> currentMap = ConfigHolder.COFIG_MAP;
        String[] parts = key.split("\\.");

        for(int i = 0; i < parts.length; i++){
            String part = parts[i];

            if(currentMap == null){
                return null;
            }

            Object node = currentMap.get(part);

            if (i == parts.length - 1) {
                return node;
            }

            if (!(node instanceof  Map)) {
                return null;
            }

            currentMap = (Map<String, Object>) node;
        }
        return null;
    }

    public static int getInt(String key, int defaultValue){
        Object value = getValue(key);

        if (value == null) {
            return defaultValue;
        }
        try {
            if (value instanceof Integer) {
                return (Integer) value;
            }
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static String getString(String key, String defaultValue) {
        Object value = getValue(key);
        if (value != null) {
            // value 可能是 String, Integer, Boolean...
            // .toString() 是最安全的转换方式
            return value.toString();
        }
        return defaultValue;
    }
}
