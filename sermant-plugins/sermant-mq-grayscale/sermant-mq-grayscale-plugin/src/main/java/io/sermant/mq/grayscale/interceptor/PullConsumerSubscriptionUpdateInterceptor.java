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
import io.sermant.core.utils.ReflectUtils;
import io.sermant.core.utils.StringUtils;
import io.sermant.mq.grayscale.service.MqConsumerGroupAutoCheck;
import io.sermant.mq.grayscale.utils.MqGrayscaleConfigUtils;
import io.sermant.mq.grayscale.utils.SubscriptionDataUtils;

import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.impl.consumer.DefaultMQPullConsumerImpl;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.common.protocol.heartbeat.SubscriptionData;

import java.util.Locale;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * update pull consumer subscription SQL92 query statement interceptor
 *
 * @author chengyouling
 * @since 2024-05-27
 **/
public class PullConsumerSubscriptionUpdateInterceptor extends AbstractInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        return context;
    }

    @Override
    public ExecuteContext after(ExecuteContext context) throws Exception {
        if (MqGrayscaleConfigUtils.isPlugEnabled()) {
            SubscriptionData subscriptionData = (SubscriptionData) context.getResult();
            if (!SubscriptionDataUtils.EXPRESSION_TYPE_SQL92.equals(subscriptionData.getExpressionType())
                    && !SubscriptionDataUtils.EXPRESSION_TYPE_TAG.equals(subscriptionData.getExpressionType())) {
                LOGGER.warning(String.format(Locale.ENGLISH, "can not process expressionType: %s",
                        subscriptionData.getExpressionType()));
                return context;
            }
            Optional<Object> fieldValue = ReflectUtils.getFieldValue(context.getObject(), "mQClientFactory");
            DefaultMQPullConsumer pullConsumer
                    = ((DefaultMQPullConsumerImpl) context.getObject()).getDefaultMQPullConsumer();
            String consumerGroup = pullConsumer.getConsumerGroup();
            String namesrvAddr = "";
            if (fieldValue.isPresent()) {
                namesrvAddr = ((MQClientInstance) fieldValue.get()).getClientConfig().getNamesrvAddr();
                if (StringUtils.isEmpty(MqGrayscaleConfigUtils.getGrayGroupTag())) {
                    MqConsumerGroupAutoCheck.setMqClientInstance(subscriptionData.getTopic(), consumerGroup,
                            (MQClientInstance) fieldValue.get());
                }
            }
            String addrTopicGroupKey = SubscriptionDataUtils.buildAddrTopicGroupKey(subscriptionData.getTopic(),
                    consumerGroup, namesrvAddr);
            SubscriptionDataUtils.resetsSql92SubscriptionData(subscriptionData, addrTopicGroupKey);
        }
        return context;
    }
}
