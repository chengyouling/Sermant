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

package io.sermant.mq.grayscale.config;

import io.sermant.core.utils.StringUtils;
import io.sermant.mq.grayscale.utils.SubscriptionDataUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * mqGrayscaleConfig entry
 *
 * @author chengyouling
 * @since 2024-05-27
 **/
public class MqGrayscaleConfig {
    private boolean enabled = false;

    private List<GrayTagItem> grayscale = new ArrayList<>();

    private BaseMessage base;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public BaseMessage getBase() {
        return base;
    }

    public void setBase(BaseMessage base) {
        this.base = base;
    }

    public List<GrayTagItem> getGrayscale() {
        return grayscale;
    }

    public void setGrayscale(List<GrayTagItem> grayscale) {
        this.grayscale = grayscale;
    }

    /**
     * return the corresponding traffic label based on serviceMeta matching result
     *
     * @param microServiceProperties serviceMeta
     * @return traffic tags
     */
    public Map<String, String> getGrayTagsByServiceMeta(Map<String, String> microServiceProperties) {
        Map<String, String> map = new HashMap<>();
        for (GrayTagItem grayTagItem : getGrayscale()) {
            if (grayTagItem.serviceMetaMatchProperties(microServiceProperties)
                    && !grayTagItem.getTrafficTag().isEmpty()) {
                // set item traffic tags when serviceMeta match, because all message tag using traffic tags.
                map.putAll(grayTagItem.getTrafficTag());
            }
        }
        return map;
    }

    /**
     * return the corresponding traffic label based on traffic properties matching result
     *
     * @param trafficProperties traffic
     * @return traffic tags
     */
    public Map<String, String> getGrayTagsByTrafficTag(Map<String, String> trafficProperties) {
        Map<String, String> map = new HashMap<>();
        for (GrayTagItem item: getGrayscale()) {
            String envKey = item.trafficMatchProperties(trafficProperties);
            if (!StringUtils.isEmpty(envKey)) {
                map.put(envKey, item.getTrafficTag().get(envKey));
            }
        }
        return map;
    }

    /**
     * return the traffic tag item based on traffic properties matching result
     *
     * @param properties traffic
     * @return gray tag item
     */
    public Optional<GrayTagItem> matchGrayTagByTrafficProperties(Map<String, String> properties) {
        for (GrayTagItem item : getGrayscale()) {
            if (!StringUtils.isEmpty(item.trafficMatchProperties(properties))) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    /**
     * return the traffic tag item based on serviceMeta properties matching result
     *
     * @param properties serviceMeta
     * @return gray tag item
     */
    public Optional<GrayTagItem> matchGrayTagByServiceMeta(Map<String, String> properties) {
        for (GrayTagItem item : getGrayscale()) {
            if (item.serviceMetaMatchProperties(properties)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    /**
     * return the traffic tag item by grayGroupTag
     *
     * @param grayGroupTag grayGroupTag
     * @return gray tag item
     */
    public Optional<GrayTagItem> getGrayTagByGroupTag(String grayGroupTag) {
        for (GrayTagItem item: getGrayscale()) {
            if (grayGroupTag.equals(item.getConsumerGroupTag())) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    /**
     * build traffic tag properties to string
     *
     * @return traffic tag string
     */
    public String buildAllTrafficTagInfoToStr() {
        StringBuilder sb = new StringBuilder();
        for (GrayTagItem item : getGrayscale()) {
            if (sb.length() > 0) {
                sb.append(SubscriptionDataUtils.AFA_SYMBOL);
            }
            sb.append(item.getConsumerGroupTag());
            for (Map.Entry<String, String> entry : item.getTrafficTag().entrySet()) {
                sb.append(entry.getKey())
                        .append(SubscriptionDataUtils.AFA_SYMBOL)
                        .append(entry.getValue());
            }
        }
        return sb.toString();
    }

    /**
     * compare source/target MqGrayscaleConfig excludeGroupTags config whether to change
     *
     * @param target MqGrayscaleConfig
     * @return isChanged
     */
    public boolean isBaseExcludeGroupTagsChanged(MqGrayscaleConfig target) {
        HashSet<String> targetBaseExcludeTags = new HashSet<>(target.getBase().getExcludeGroupTags());
        HashSet<String> sourceBaseExcludeTags = new HashSet<>(getBase().getExcludeGroupTags());
        targetBaseExcludeTags.removeAll(sourceBaseExcludeTags);
        target.getBase().getExcludeGroupTags().forEach(sourceBaseExcludeTags::remove);
        return !targetBaseExcludeTags.isEmpty() || !sourceBaseExcludeTags.isEmpty();
    }

    /**
     * compare source/target MqGrayscaleConfig consumerType config whether to change
     *
     * @param target MqGrayscaleConfig
     * @return isChanged
     */
    public boolean isConsumerTypeChanged(MqGrayscaleConfig target) {
        String sourceType = getBase() == null ? "" : getBase().getConsumeType();
        String targetType = target.getBase() == null ? "" : target.getBase().getConsumeType();
        return !sourceType.equals(targetType);
    }

    /**
     * update base info/traffic tags
     *
     * @param config config
     */
    public void updateGrayscaleConfig(MqGrayscaleConfig config) {
        setBase(config.getBase());
        if (config.getGrayscale().isEmpty()) {
            return;
        }
        for (GrayTagItem item : getGrayscale()) {
            item.updateTrafficTags(config.getGrayscale());
        }
    }
}
