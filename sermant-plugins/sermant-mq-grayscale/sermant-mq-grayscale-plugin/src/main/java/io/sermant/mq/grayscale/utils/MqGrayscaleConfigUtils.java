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
import io.sermant.mq.grayscale.config.Base;
import io.sermant.mq.grayscale.config.MessageFilter;
import io.sermant.mq.grayscale.config.MqGrayscaleConfig;
import io.sermant.mq.grayscale.strategy.TagKeyMatcher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * grayscale config util
 *
 * @author chengyouling
 * @since 2024-06-03
 */
public class MqGrayscaleConfigUtils {
    public final static String MICRO_SERVICE_GRAY_TAG_KEY = "micro_service_gray_tag";

    public final static String MICRO_TRAFFIC_GRAY_TAG_KEY = "micro_traffic_gray_tag";

    public static boolean MQ_EXCLUDE_TAGS_CHANGE_FLAG = false;

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

    public static String getGrayEnvTag() {
        if (grayscaleDisabled()) {
            return "";
        }
        Map<String, List<String>> envMatch= configCache.get(CONFIG_CACHE_KEY).getGrayscale().getEnvironmentMatch();
        if (envMatch == null) {
            return "";
        }
        String matchTag = TagKeyMatcher.getMatchTag(envMatch, MICRO_SERVICE_PROPERTIES);
        if (!StringUtils.isEmpty(matchTag)) {
            return standardFormatTag(matchTag);
        }
        return "";
    }

    public static String getTrafficGrayTag() {
        if (grayscaleDisabled()) {
            return "";
        }
        Map<String, List<String>> envMatch = configCache.get(CONFIG_CACHE_KEY).getGrayscale().getTrafficMatch();
        if (envMatch == null) {
            return "";
        }
        String matchTag = TagKeyMatcher.getMatchTag(envMatch, buildTrafficTag());
        if (!StringUtils.isEmpty(matchTag)) {
            return standardFormatTag(matchTag);
        }
        return "";
    }

    public static boolean isExcludeTagsContainsTag(String grayTag) {
        if (StringUtils.isEmpty(grayTag)) {
            return false;
        }
        if (!grayBaseDisabled()) {
            Map<String, String> excludeTags
                    = configCache.get(CONFIG_CACHE_KEY).getBase().getMessageFilter().getExcludeTags();
            if (!excludeTags.isEmpty()) {
                for (Map.Entry<String, String> entry: excludeTags.entrySet()) {
                    if (grayTag.equals(standardFormatTag(entry.getKey() + "%" + entry.getValue()))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void modifyExcludeTags(Set<String> excludeTags) {
        if (excludeTags.isEmpty()) {
            return;
        }
        MQ_EXCLUDE_TAGS_CHANGE_FLAG = true;
        if (configCache.get(CONFIG_CACHE_KEY).getBase() == null) {
            configCache.get(CONFIG_CACHE_KEY).setBase(new Base());
        }
        if (configCache.get(CONFIG_CACHE_KEY).getBase().getMessageFilter() == null) {
            configCache.get(CONFIG_CACHE_KEY).getBase().setMessageFilter(new MessageFilter());
        }
        Map<String, String> configExcludeTags
                = configCache.get(CONFIG_CACHE_KEY).getBase().getMessageFilter().getExcludeTags();
        for (String tag: excludeTags) {
            String tagName = tag.split("%")[0];
            String tagValue = tag.split("%")[1];
            configExcludeTags.put(tagName, tagValue);
        }
    }

    public static String getConsumeType() {
        if (grayBaseDisabled()) {
            return "all";
        }
        return configCache.get(CONFIG_CACHE_KEY).getBase().getMessageFilter().getConsumeType();
    }

    public static long getAutoCheckDelayTime() {
        if (grayBaseDisabled()) {
            return 30L;
        }
        return configCache.get(CONFIG_CACHE_KEY).getBase().getMessageFilter().getAutoCheckDelayTime();
    }

    public static String standardFormatTag(String tag) {
        return tag.toLowerCase(Locale.ROOT).replaceAll("[^%|a-zA-Z0-9_-]", "-");
    }

    public static Set<String> getExcludeTagsForSet() {
        Set<String> excludeTags = new HashSet<>();
        if (grayBaseDisabled()) {
            return excludeTags;
        }
        Map<String, String> tags =
                configCache.get(CONFIG_CACHE_KEY).getBase().getMessageFilter().getExcludeTags();
        for (Map.Entry<String, String> entry: tags.entrySet()) {
            excludeTags.add(standardFormatTag(entry.getKey() + "%" + entry.getValue()));
        }
        return excludeTags;
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

    private static boolean grayscaleDisabled() {
        return !configCache.get(CONFIG_CACHE_KEY).isEnabled()
                || configCache.get(CONFIG_CACHE_KEY).getGrayscale() == null;
    }

    private static boolean grayBaseDisabled() {
        return configCache.get(CONFIG_CACHE_KEY).getBase() == null
                || configCache.get(CONFIG_CACHE_KEY).getBase().getMessageFilter() == null;
    }

    public static void resetGrayscaleConfig() {
        configCache.put(CONFIG_CACHE_KEY, new MqGrayscaleConfig());
    }

    public static void setGrayscaleConfig(MqGrayscaleConfig config) {
        configCache.put(CONFIG_CACHE_KEY, config);
    }

    public static boolean isPlugEnabled() {
        return configCache.get(CONFIG_CACHE_KEY).isEnabled();
    }
}
