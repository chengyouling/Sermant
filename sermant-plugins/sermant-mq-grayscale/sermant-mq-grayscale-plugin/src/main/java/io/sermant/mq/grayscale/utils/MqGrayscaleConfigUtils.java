/*
 * Copyright (C) 2024-2024 Sermant Authors. All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.sermant.mq.grayscale.utils;

import io.sermant.core.config.ConfigManager;
import io.sermant.core.plugin.config.ServiceMeta;
import io.sermant.core.utils.StringUtils;
import io.sermant.core.utils.tag.TrafficUtils;
import io.sermant.mq.grayscale.config.GrayTagItem;
import io.sermant.mq.grayscale.config.MqGrayscaleConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.rocketmq.common.message.Message;

/**
 * grayscale config util
 *
 * @author chengyouling
 * @since 2024-06-03
 */
public class MqGrayscaleConfigUtils {
    public static boolean MQ_GRAY_TAGS_CHANGE_FLAG = false;

    public final static Map<String, String> MICRO_SERVICE_PROPERTIES = new HashMap<>();

    private final static Map<String, MqGrayscaleConfig> configCache = new ConcurrentHashMap<>();

    public final static String CONFIG_CACHE_KEY = "mqGrayConfig";

    static {
        ServiceMeta serviceMeta = ConfigManager.getConfig(ServiceMeta.class);
        MICRO_SERVICE_PROPERTIES.put("version", serviceMeta.getVersion());
        if (serviceMeta.getParameters() != null) {
            MICRO_SERVICE_PROPERTIES.putAll(serviceMeta.getParameters());
        }
        configCache.put(CONFIG_CACHE_KEY, new MqGrayscaleConfig());
    }

    private MqGrayscaleConfigUtils() {}

    public static String getGrayGroupTag() {
        if (!configCache.get(CONFIG_CACHE_KEY).isEnabled()) {
            return "";
        }
        MqGrayscaleConfig mqGrayscaleConfig = configCache.get(CONFIG_CACHE_KEY);
        if (mqGrayscaleConfig == null) {
            return "";
        }
        GrayTagItem item = matchGrayTagByProperties(buildTrafficTag(), mqGrayscaleConfig.getGrayscale());
        if (item == null) {
            item = matchGrayTagByProperties(MICRO_SERVICE_PROPERTIES, mqGrayscaleConfig.getGrayscale());
        }
        if (item != null) {
            return standardFormatTag(item.getConsumerGroupTag());
        }
        return "";
    }

    public static String getConsumeType() {
        if (configCache.get(CONFIG_CACHE_KEY).getBase() == null) {
            return "all";
        }
        return configCache.get(CONFIG_CACHE_KEY).getBase().getConsumeType();
    }

    public static long getAutoCheckDelayTime() {
        if (configCache.get(CONFIG_CACHE_KEY).getBase() == null) {
            return 30L;
        }
        return configCache.get(CONFIG_CACHE_KEY).getBase().getAutoCheckDelayTime();
    }

    public static String standardFormatTag(String tag) {
        return tag.toLowerCase(Locale.ROOT).replaceAll("[^%|a-zA-Z0-9_-]", "-");
    }

    private static Map<String, String> buildTrafficTag() {
        Map<String, String> map = new HashMap<>();
        if (TrafficUtils.getTrafficTag() != null && TrafficUtils.getTrafficTag().getTag() != null) {
            Map<String, List<String>> trafficTags = TrafficUtils.getTrafficTag().getTag();
            for (Map.Entry<String, List<String>> entry: trafficTags.entrySet()) {
                map.put(entry.getKey(), entry.getValue().get(0));
            }
        }
        return map;
    }

    public static boolean isMqServerGrayEnabled() {
        return configCache.get(CONFIG_CACHE_KEY).isServerGrayEnabled();
    }

    public static void resetGrayscaleConfig() {
        configCache.put(CONFIG_CACHE_KEY, new MqGrayscaleConfig());
    }

    public static void setGrayscaleConfig(MqGrayscaleConfig config) {
        MQ_GRAY_TAGS_CHANGE_FLAG = true;
        configCache.put(CONFIG_CACHE_KEY, config);
    }

    public static boolean isPlugEnabled() {
        return configCache.get(CONFIG_CACHE_KEY).isEnabled();
    }

    public static void setUserPropertyByEnvTag(Message message) {
        if (!configCache.get(CONFIG_CACHE_KEY).isEnabled()) {
            return;
        }
        MqGrayscaleConfig mqGrayscaleConfig = configCache.get(CONFIG_CACHE_KEY);
        if (mqGrayscaleConfig == null) {
            return;
        }
        Map<String, String> envTags = getMatchEnvTag(mqGrayscaleConfig.getGrayscale());
        if (envTags.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : envTags.entrySet()) {
            message.putUserProperty(entry.getKey(), entry.getValue());
        }
    }

    public static void setUserPropertyByTrafficTag(Message message) {
        if (!configCache.get(CONFIG_CACHE_KEY).isEnabled() || buildTrafficTag().isEmpty()) {
            return;
        }
        MqGrayscaleConfig mqGrayscaleConfig = configCache.get(CONFIG_CACHE_KEY);
        if (mqGrayscaleConfig == null) {
            return;
        }
        Map<String, String> envTags = getMatchTrafficTag(mqGrayscaleConfig.getGrayscale());
        if (envTags.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : envTags.entrySet()) {
            message.putUserProperty(entry.getKey(), entry.getValue());
        }
    }

    public static MqGrayscaleConfig getGrayscaleConfigs() {
        return configCache.get(CONFIG_CACHE_KEY);
    }

    public static Map<String, String> getMatchEnvTag(List<GrayTagItem> grayTagItems) {
        Map<String, String> map = new HashMap<>();
        for (GrayTagItem grayTagItem : grayTagItems) {
            String envKey = envMatchProperties(MICRO_SERVICE_PROPERTIES, grayTagItem);
            if (!StringUtils.isEmpty(envKey)) {
                map.put(envKey, grayTagItem.getEnvTag().get(envKey));
            }
        }
        return map;
    }

    public static Map<String, String> getMatchTrafficTag(List<GrayTagItem> grayscale) {
        Map<String, String> map = new HashMap<>();
        for (GrayTagItem item: grayscale) {
            String envKey = trafficMatchProperties(buildTrafficTag(), item);
            if (!StringUtils.isEmpty(envKey)) {
                map.put(envKey, item.getTrafficTag().get(envKey));
            }
        }
        return map;
    }

    private static String trafficMatchProperties(Map<String, String> properties, GrayTagItem item) {
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (item.getTrafficTag().containsKey(entry.getKey())
                && item.getTrafficTag().get(entry.getKey()).equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static GrayTagItem matchGrayTagByProperties(Map<String, String> properties, List<GrayTagItem> grayscale) {
        for (GrayTagItem item : grayscale) {
            if (!StringUtils.isEmpty(envMatchProperties(properties, item))) {
                return item;
            }
        }
        return null;
    }

    private static String envMatchProperties(Map<String, String> properties, GrayTagItem item) {
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (item.getEnvTag().containsKey(entry.getKey())
                && item.getEnvTag().get(entry.getKey()).equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static Set<String> buildGrayTagKeySet(List<GrayTagItem> grayTagItems) {
        Set<String> set = new HashSet<>();
        for (GrayTagItem item : grayTagItems) {
            if (!item.getEnvTag().isEmpty()) {
                set.addAll(item.getEnvTag().keySet());
            }
            if (!item.getTrafficTag().isEmpty()) {
                set.addAll(item.getTrafficTag().keySet());
            }
        }
        return set;
    }

    public static GrayTagItem getScaleByGroupTag(String groupTag, List<GrayTagItem> items) {
        for (GrayTagItem item: items) {
            if (groupTag.equals(item.getConsumerGroupTag())) {
                return item;
            }
        }
        return null;
    }

    public static String chooseTagAsBasicSqlTag() {
        MqGrayscaleConfig configs = getGrayscaleConfigs();
        if (configs != null && !configs.getGrayscale().isEmpty()) {
            for (GrayTagItem item : configs.getGrayscale()) {
                if (!item.getTrafficTag().isEmpty()) {
                    return (String) item.getTrafficTag().keySet().toArray()[0];
                }
                if (!item.getEnvTag().isEmpty()) {
                    return (String) item.getEnvTag().keySet().toArray()[0];
                }
            }
        }
        return "base_tag";
    }
}
