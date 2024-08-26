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
import io.sermant.mq.grayscale.config.MqConsumerClientConfig;
import io.sermant.mq.grayscale.utils.MqGrayscaleConfigUtils;
import io.sermant.mq.grayscale.utils.SubscriptionDataUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.MQClientAPIImpl;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.common.protocol.body.GroupList;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();

    private static final String CONSUME_TYPE_AUTO = "auto";

    /**
     * gray consumer tags at last time
     * key: namesrvAddr@topic@consumerGroup
     * value: consumer gray tags
     */
    private static final Map<String, Set<String>> LAST_TOPIC_GROUP_GRAY_TAG = new HashMap<>();

    /**
     * client configs for query consumer group
     * key: namesrvAddr@topic@consumerGroup
     * value: client config
     */
    private static final Map<String, MqConsumerClientConfig> CONSUMER_CLIENT_CONFIG_MAP = new HashMap<>();

    private static final AtomicBoolean START_AUTO_CHECK = new AtomicBoolean(false);

    private static final long INITIAL_DELAY = 10L;

    private static final long ROCKET_MQ_READ_TIMEOUT = 5000L;

    private MqConsumerGroupAutoCheck() {
    }

    /**
     * set current client MQClientInstance info
     *
     * @param topic topic
     * @param consumerGroup consumerGroup
     * @param mqClientInstance mqClientInstance
     */
    public static void setMqClientInstance(String topic, String consumerGroup, MQClientInstance mqClientInstance) {
        String addrTopicGroupKey = SubscriptionDataUtils.buildAddrTopicGroupKey(topic, consumerGroup,
                mqClientInstance.getClientConfig().getNamesrvAddr());
        if (CONSUMER_CLIENT_CONFIG_MAP.get(addrTopicGroupKey) != null
                && CONSUMER_CLIENT_CONFIG_MAP.get(addrTopicGroupKey).getMqClientInstance() == null) {
            CONSUMER_CLIENT_CONFIG_MAP.get(addrTopicGroupKey).setMqClientInstance(mqClientInstance);
        }
        if (CONSUME_TYPE_AUTO.equals(MqGrayscaleConfigUtils.getConsumeType())) {
            // sync to obtain current gray consumer group at service start
            schedulerCheck();

            // async to check gray consumer group
            if (START_AUTO_CHECK.compareAndSet(false, true)) {
                EXECUTOR_SERVICE.scheduleWithFixedDelay(MqConsumerGroupAutoCheck::schedulerCheck, INITIAL_DELAY,
                        MqGrayscaleConfigUtils.getAutoCheckDelayTime(), TimeUnit.SECONDS);
            }
        }
    }

    private static void schedulerCheck() {
        if (CONSUMER_CLIENT_CONFIG_MAP.isEmpty()) {
            return;
        }
        if (!StringUtils.isEmpty(MqGrayscaleConfigUtils.getGrayGroupTag())) {
            return;
        }
        if (MqGrayscaleConfigUtils.getGrayscaleConfigs() == null
                || MqGrayscaleConfigUtils.getGrayscaleConfigs().getGrayscale().isEmpty()) {
            return;
        }
        for (MqConsumerClientConfig clientConfig : CONSUMER_CLIENT_CONFIG_MAP.values()) {
            if (clientConfig.getMqClientInstance() == null) {
                continue;
            }
            findGrayConsumerGroup(clientConfig);
        }
    }

    /**
     * querying all consumer groups of Topic and Collecting grayGroupTag
     *
     * @param clientConfig clientConfig
     */
    private static void findGrayConsumerGroup(MqConsumerClientConfig clientConfig) {
        try {
            MQClientAPIImpl mqClientApi = clientConfig.getMqClientInstance().getMQClientAPIImpl();
            TopicRouteData topicRouteData = mqClientApi.getTopicRouteInfoFromNameServer(clientConfig.getTopic(),
                    ROCKET_MQ_READ_TIMEOUT, false);
            List<String> brokerList = new ArrayList<>();
            for (BrokerData brokerData : topicRouteData.getBrokerDatas()) {
                brokerList.addAll(brokerData.getBrokerAddrs().values());
            }
            String brokerAddress = brokerList.get(0);
            Set<String> grayTags = new HashSet<>();
            GroupList groupList = mqClientApi.queryTopicConsumeByWho(brokerAddress, clientConfig.getTopic(),
                    ROCKET_MQ_READ_TIMEOUT);
            for (String group : groupList.getGroupList()) {
                try {
                    List<String> consumerIds = mqClientApi.getConsumerIdListByGroup(brokerAddress, group,
                            ROCKET_MQ_READ_TIMEOUT);
                    String grayTag = StringUtils.substringAfterLast(group, clientConfig.getConsumerGroup() + "_");
                    if (!consumerIds.isEmpty() && !StringUtils.isEmpty(grayTag)) {
                        grayTags.add(grayTag);
                    }
                } catch (RemotingConnectException | RemotingSendRequestException | RemotingTimeoutException
                         | MQBrokerException | InterruptedException e) {
                    LOGGER.warning(String.format(Locale.ENGLISH, "[auto-check] can not find ids in group: [%s].",
                            group));
                }
            }
            resetAutoCheckGrayTagItems(grayTags, clientConfig);
        } catch (MQClientException | InterruptedException | RemotingTimeoutException | RemotingSendRequestException
                 | RemotingConnectException | MQBrokerException e) {
            LOGGER.log(Level.FINE, String.format(Locale.ENGLISH, "[auto-check] error, message: %s",
                    e.getMessage()), e);
        }
    }

    /**
     * compare current query grayGroupTag with collected last time, reset autoCheckGrayTagItems
     *
     * @param grayTags grayTags
     * @param clientConfig MqConsumerClientConfig
     */
    private static void resetAutoCheckGrayTagItems(Set<String> grayTags, MqConsumerClientConfig clientConfig) {
        String addrTopicGroupKey = SubscriptionDataUtils.buildAddrTopicGroupKey(clientConfig.getTopic(),
                clientConfig.getConsumerGroup(), clientConfig.getAddress());
        if (grayTags.isEmpty()) {
            if (LAST_TOPIC_GROUP_GRAY_TAG.containsKey(addrTopicGroupKey)) {
                SubscriptionDataUtils.resetAutoCheckGrayTagItems(new ArrayList<>(), clientConfig);
                LAST_TOPIC_GROUP_GRAY_TAG.remove(addrTopicGroupKey);
            }
            return;
        }
        HashSet<String> currentGroups = new HashSet<>(grayTags);
        if (LAST_TOPIC_GROUP_GRAY_TAG.containsKey(addrTopicGroupKey)) {
            currentGroups.removeAll(LAST_TOPIC_GROUP_GRAY_TAG.get(addrTopicGroupKey));
        }
        List<GrayTagItem> grayTagItems = new ArrayList<>();
        if (!currentGroups.isEmpty() || grayTags.size() != LAST_TOPIC_GROUP_GRAY_TAG.get(addrTopicGroupKey).size()) {
            for (String grayTag : grayTags) {
                LOGGER.log(Level.INFO, String.format(Locale.ENGLISH, "[auto-check] current find gray tag: [%s].",
                        grayTag));
                Optional<GrayTagItem> item = MqGrayscaleConfigUtils.getGrayscaleConfigs().getGrayTagByGroupTag(grayTag);
                item.ifPresent(grayTagItems::add);
            }
            LAST_TOPIC_GROUP_GRAY_TAG.put(addrTopicGroupKey, grayTags);
            SubscriptionDataUtils.resetAutoCheckGrayTagItems(grayTagItems, clientConfig);
        }
    }

    /**
     * set consumer client config
     *
     * @param address address
     * @param topic topic
     * @param consumerGroup consumerGroup
     */
    public static void setConsumerClientConfig(String address, String topic, String consumerGroup) {
        MqConsumerClientConfig config = new MqConsumerClientConfig(address, topic, consumerGroup);
        String addrTopicGroupKey = SubscriptionDataUtils.buildAddrTopicGroupKey(topic, consumerGroup, address);
        if (!CONSUMER_CLIENT_CONFIG_MAP.containsKey(addrTopicGroupKey)) {
            CONSUMER_CLIENT_CONFIG_MAP.put(addrTopicGroupKey, config);
            SubscriptionDataUtils.setAutoCheckTagChangeMap(address, topic, consumerGroup, true);
        }
    }
}
