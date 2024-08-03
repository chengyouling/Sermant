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

package io.sermant.mq.grayscale.interceptor;

import io.sermant.core.common.LoggerFactory;
import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.core.plugin.agent.interceptor.AbstractInterceptor;
import io.sermant.core.utils.StringUtils;
import io.sermant.mq.grayscale.service.MqConsumerGroupAutoCheck;
import io.sermant.mq.grayscale.utils.MqGrayscaleConfigUtils;
import io.sermant.mq.grayscale.utils.SubscriptionDataUtils;

import org.apache.rocketmq.client.impl.consumer.RebalanceImpl;
import org.apache.rocketmq.common.protocol.heartbeat.SubscriptionData;

import java.util.Locale;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * TAG/SQL92 query message statement interceptor
 *
 * @author chengyouling
 * @since 2024-05-27
 **/
public class MqSubscriptionAutoCheckInterceptor extends AbstractInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        return context;
    }

    @Override
    public ExecuteContext after(ExecuteContext context) throws Exception {
        if (MqGrayscaleConfigUtils.isPlugEnabled() && MqGrayscaleConfigUtils.isMqServerGrayEnabled()) {
            ConcurrentMap<String, SubscriptionData> map = (ConcurrentMap<String, SubscriptionData>) context.getResult();
            RebalanceImpl balance = (RebalanceImpl) context.getObject();
            for (SubscriptionData subscriptionData : map.values()) {
                if(buildSql92SubscriptionData(subscriptionData, balance)) {
                    SubscriptionDataUtils.resetTagChangeMap(subscriptionData.getTopic(),
                            balance.getConsumerGroup(), false);
                }
            }
        }
        return context;
    }

    private boolean buildSql92SubscriptionData(SubscriptionData subscriptionData, RebalanceImpl balance) {
        if (balance.getConsumerGroup() != null
                && SubscriptionDataUtils.checkTopicGroupTag(subscriptionData.getTopic(), balance.getConsumerGroup())) {
            if (StringUtils.isEmpty(MqGrayscaleConfigUtils.getGrayGroupTag())) {
                MqConsumerGroupAutoCheck.setMqClientInstance(subscriptionData.getTopic(), balance.getConsumerGroup(),
                        balance.getmQClientFactory());
            }
            String topicGroupKey = SubscriptionDataUtils.buildTopicGroupKey(subscriptionData.getTopic(),
                    balance.getConsumerGroup());
            if (SubscriptionDataUtils.EXPRESSION_TYPE_TAG.equals(subscriptionData.getExpressionType())) {
                SubscriptionDataUtils.resetsSQL92SubscriptionData(subscriptionData, topicGroupKey);
                return true;
            } else if (SubscriptionDataUtils.EXPRESSION_TYPE_SQL92.equals(subscriptionData.getExpressionType())) {
                String originSubData = subscriptionData.getSubString();
                String subStr = SubscriptionDataUtils.addMseGrayTagsToSQL92Expression(originSubData, topicGroupKey);
                if (StringUtils.isEmpty(subStr)) {
                    subStr = "( _message_tag_ is null ) or ( _message_tag_ is not null )";
                }
                subscriptionData.setSubString(subStr);
                subscriptionData.setSubVersion(System.currentTimeMillis());
                LOGGER.warning(String.format(Locale.ENGLISH, "update SQL92 subscriptionData, "
                        + "originSubStr: %s, newSubStr: %s", originSubData, subStr));
                return true;
            } else {
                LOGGER.warning(String.format(Locale.ENGLISH, "can not process expressionType: %s",
                        subscriptionData.getExpressionType()));
            }
        }
        return false;
    }
}
