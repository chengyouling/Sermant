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

import io.sermant.mq.grayscale.utils.MqGrayscaleConfigUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.impl.MQClientAPIImpl;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.common.protocol.body.GroupList;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * consumer group auto check service
 *
 * @author chengyouling
 * @since 2024-05-27
 **/
public class MqConsumerGroupAutoCheck {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqConsumerGroupAutoCheck.class);

    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private static final String CONSUME_TYPE_AUTO = "auto";

    private static MQClientInstance BASE_MQ_CLIENT_INSTANCE = null;

    private static String BASE_ORIGIN_GROUP = null;

    private static String TOPIC = null;

    private static HashSet<String> lastAvailableGroup = new HashSet<>();

    static {
        executorService.scheduleWithFixedDelay(
                MqConsumerGroupAutoCheck::schedulerCheckGrayConsumerStart,  0L,
                MqGrayscaleConfigUtils.getAutoCheckDelayTime(), TimeUnit.SECONDS);
    }

    public static void setMqClientInstance(MQClientInstance mQClientInstance) {
        BASE_MQ_CLIENT_INSTANCE = mQClientInstance;
    }

    public static void setOriginGroup(String originGroup) {
        BASE_ORIGIN_GROUP = originGroup;
    }

    public static void setTopic(String topic) {
        TOPIC = topic;
    }

    private static void schedulerCheckGrayConsumerStart() {
        if (TOPIC == null || BASE_MQ_CLIENT_INSTANCE == null) {
            return;
        }
        if (!CONSUME_TYPE_AUTO.equals(MqGrayscaleConfigUtils.getConsumeType())) {
            return;
        }
        try {
            MQClientAPIImpl mqClientAPI = BASE_MQ_CLIENT_INSTANCE.getMQClientAPIImpl();
            TopicRouteData topicRouteData = mqClientAPI.getTopicRouteInfoFromNameServer(TOPIC, 5000L, false);
            List<String> brokerList = new ArrayList<>();
            for (BrokerData brokerData : topicRouteData.getBrokerDatas()) {
                brokerList.addAll(brokerData.getBrokerAddrs().values());
            }
            String brokerAddress = brokerList.get(0);
            Set<String> availableGroup = new HashSet<>();
            GroupList groupList = mqClientAPI.queryTopicConsumeByWho(brokerAddress, TOPIC, 5000L);
            LOGGER.warn("auto check gray consumer, fined groups:{}", groupList.getGroupList());
            for (String group : groupList.getGroupList()) {
                try {
                    List<String> consumerIds = mqClientAPI.getConsumerIdListByGroup(brokerAddress, group, 15000L);
                    LOGGER.warn("auto check gray consumer, current group: {}, contains consumerIds:{}", group,
                            consumerIds);
                    if (!consumerIds.isEmpty()) {
                        availableGroup.add(group);
                    }
                } catch (Exception e) {
                    LOGGER.error("auto check gray consumer, get consumerIds error, group: {}", group, e);
                }
            }
            modifyConsumerExcludeTags(availableGroup);
        } catch (Exception e) {
            LOGGER.error("auto check gray consumer error, message: {}", e.getMessage(), e);
        }
    }

    private static void modifyConsumerExcludeTags(Set<String> availableGroup) {
        HashSet<String> currentGroups = new HashSet<>(availableGroup);
        HashSet<String> lastGroups = new HashSet<>(lastAvailableGroup);
        currentGroups.removeAll(lastAvailableGroup);
        lastGroups.removeAll(availableGroup);
        if (!currentGroups.isEmpty() || !lastGroups.isEmpty()) {
            Set<String> excludeTag = new HashSet<>();
            for (String group : availableGroup) {
                if (!group.equals(BASE_ORIGIN_GROUP)) {
                    String env = StringUtils.substringAfterLast(group, BASE_ORIGIN_GROUP + "_");
                    if (StringUtils.isNotEmpty(env))
                        excludeTag.add(env);
                }
            }
            LOGGER.warn("auto check gray consumer, current lastAvailableGroup: {}", lastAvailableGroup);
            lastAvailableGroup = new HashSet<>(availableGroup);
            MqGrayscaleConfigUtils.modifyExcludeTags(excludeTag);
        }
    }
}
