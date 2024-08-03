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

package io.sermant.mq.grayscale.service;

import io.sermant.core.common.LoggerFactory;
import io.sermant.mq.grayscale.config.GrayTagItem;
import io.sermant.mq.grayscale.utils.MqGrayscaleConfigUtils;
import io.sermant.mq.grayscale.utils.SubscriptionDataUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.impl.MQClientAPIImpl;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.common.protocol.body.GroupList;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * consumer group auto check service
 *
 * @author chengyouling
 * @since 2024-05-27
 **/
public class MqConsumerGroupAutoCheck {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private static final String CONSUME_TYPE_AUTO = "auto";

    private static MQClientInstance BASE_MQ_CLIENT_INSTANCE = null;

    private static String BASE_ORIGIN_GROUP = null;

    private static String TOPIC = null;

    private static final Map<String, Set<String>> LAST_TOPIC_GROUP_GRAY_TAG = new HashMap<>();

    private static final Map<String, MqConsumerClientConfig> CONSUMER_CLIENT_CONFIG_MAP = new HashMap<>();

    static {
        executorService.scheduleWithFixedDelay(
                MqConsumerGroupAutoCheck::schedulerCheck,  0L,
                MqGrayscaleConfigUtils.getAutoCheckDelayTime(), TimeUnit.SECONDS);
    }

    public static void setMqClientInstance(String topic, String consumerGroup, MQClientInstance mQClientInstance) {
        topic = topic.contains("%RETRY%") ? StringUtils.substringAfterLast(topic, "%RETRY%") : topic;
        consumerGroup = consumerGroup.contains("%RETRY%")
                ? StringUtils.substringAfterLast(consumerGroup, "%RETRY%") : consumerGroup;
        String address = mQClientInstance.getClientConfig().getNamesrvAddr();
        String tempKey = address + "@" + topic + "@" + consumerGroup;
        LOGGER.warning(String.format(Locale.ENGLISH, "[auto-check] setMqClientInstance buildKey: [%s]。", tempKey));
        if (CONSUMER_CLIENT_CONFIG_MAP.get(tempKey).getMqClientInstance() == null) {
            CONSUMER_CLIENT_CONFIG_MAP.get(tempKey).setMqClientInstance(mQClientInstance);
        }
    }

    public static void setOriginGroup(String originGroup) {
        BASE_ORIGIN_GROUP = originGroup;
    }

    public static void setTopic(String topic) {
        TOPIC = topic;
    }

    private static void schedulerCheck() {
        if (CONSUMER_CLIENT_CONFIG_MAP.isEmpty()) {
            return;
        }
        if (!CONSUME_TYPE_AUTO.equals(MqGrayscaleConfigUtils.getConsumeType())) {
            return;
        }
        if (!StringUtils.isEmpty(MqGrayscaleConfigUtils.getGrayGroupTag())) {
            return;
        }
        if (MqGrayscaleConfigUtils.getGrayscaleConfigs() == null ||
            MqGrayscaleConfigUtils.getGrayscaleConfigs().getGrayscale().isEmpty()) {
            return;
        }
        for (MqConsumerClientConfig clientConfig : CONSUMER_CLIENT_CONFIG_MAP.values()) {
            if (clientConfig.getMqClientInstance() == null) {
                continue;
            }
            findGrayConsumerGroup(clientConfig);
        }
    }

    private static void findGrayConsumerGroup(MqConsumerClientConfig clientConfig) {
        try {
            MQClientAPIImpl mqClientAPI = clientConfig.getMqClientInstance().getMQClientAPIImpl();
            TopicRouteData topicRouteData
                    = mqClientAPI.getTopicRouteInfoFromNameServer(clientConfig.getTopic(), 5000L, false);
            List<String> brokerList = new ArrayList<>();
            for (BrokerData brokerData : topicRouteData.getBrokerDatas()) {
                brokerList.addAll(brokerData.getBrokerAddrs().values());
            }
            String brokerAddress = brokerList.get(0);
            Set<String> grayTags = new HashSet<>();
            GroupList groupList = mqClientAPI.queryTopicConsumeByWho(brokerAddress, clientConfig.getTopic(), 5000L);
            LOGGER.log(Level.WARNING, String.format(Locale.ENGLISH, "topic: %s, [auto-check] find groups: ",
                    clientConfig.getTopic()), groupList);
            for (String group : groupList.getGroupList()) {
                try {
                    List<String> consumerIds = mqClientAPI.getConsumerIdListByGroup(brokerAddress, group, 15000L);
                    String grayTag = StringUtils.substringAfterLast(group, clientConfig.getConsumerGroup() + "_");
                    if (!consumerIds.isEmpty() && !StringUtils.isEmpty(grayTag)) {
                        grayTags.add(grayTag);
                    }
                } catch (Exception e) {
                    LOGGER.warning(String.format(Locale.ENGLISH, "[auto-check] can not find ids in group: [%s]。", group));
                }
            }
            resetAutoCheckGrayTagItems(grayTags, clientConfig);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, String.format(Locale.ENGLISH, "[auto-check] error, message: %s",
                    e.getMessage()), e);
        }
    }

    private static void resetAutoCheckGrayTagItems(Set<String> grayTags, MqConsumerClientConfig clientConfig) {
        String topicGroupKey = SubscriptionDataUtils.buildTopicGroupKey(clientConfig.getTopic(),
                clientConfig.getConsumerGroup());
        if (grayTags.isEmpty()) {
            if (LAST_TOPIC_GROUP_GRAY_TAG.containsKey(topicGroupKey)) {
                SubscriptionDataUtils.resetAutoCheckGrayTagItems(new ArrayList<>(), clientConfig);
                LAST_TOPIC_GROUP_GRAY_TAG.remove(topicGroupKey);
            }
            return;
        }
        HashSet<String> currentGroups = new HashSet<>(grayTags);
        if (LAST_TOPIC_GROUP_GRAY_TAG.containsKey(topicGroupKey)) {
            currentGroups.removeAll(LAST_TOPIC_GROUP_GRAY_TAG.get(topicGroupKey));
        }
        List<GrayTagItem> grayTagItems = new ArrayList<>();
        if (!currentGroups.isEmpty() || grayTags.size() != LAST_TOPIC_GROUP_GRAY_TAG.get(topicGroupKey).size()) {
            for (String tag : grayTags) {
                GrayTagItem item = MqGrayscaleConfigUtils.getScaleByGroupTag(tag,
                    MqGrayscaleConfigUtils.getGrayscaleConfigs().getGrayscale());
                if (item != null) {
                    grayTagItems.add(item);
                }
            }
            LAST_TOPIC_GROUP_GRAY_TAG.put(topicGroupKey, grayTags);
            LOGGER.log(Level.INFO, "[auto-check] gray group, current gray tags: ", grayTags);
            SubscriptionDataUtils.resetAutoCheckGrayTagItems(grayTagItems, clientConfig);
        }
    }

    public static void setConsumerClientConfig(MqConsumerClientConfig config) {
        String configKey = buildConfigKey(config);
        if (!CONSUMER_CLIENT_CONFIG_MAP.containsKey(configKey)) {
            CONSUMER_CLIENT_CONFIG_MAP.put(configKey, config);
        }
    }

    private static String buildConfigKey(MqConsumerClientConfig config) {
        return config.getAddress() + "@" + config.getTopic() + "@" + config.getConsumerGroup();
    }
}
