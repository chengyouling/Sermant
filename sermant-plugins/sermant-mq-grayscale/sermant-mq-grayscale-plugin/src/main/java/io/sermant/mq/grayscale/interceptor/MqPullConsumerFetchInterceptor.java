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

import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.core.plugin.agent.interceptor.AbstractInterceptor;
import io.sermant.core.utils.StringUtils;
import io.sermant.mq.grayscale.service.MqConsumerGroupAutoCheck;
import io.sermant.mq.grayscale.utils.MqGrayscaleConfigUtils;
import io.sermant.mq.grayscale.utils.SubscriptionDataUtils;

import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.impl.consumer.DefaultMQPullConsumerImpl;

/**
 * PullConsumer fetchSubscribeMessageQueues/fetchMessageQueuesInBalance method interceptor
 * base scene recording namesrvAddr、topic、group info
 * gray scene reset consumerGroup with grayGroupTag
 *
 * @author chengyouling
 * @since 2024-05-27
 **/
public class MqPullConsumerFetchInterceptor extends AbstractInterceptor {
    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        return context;
    }

    @Override
    public ExecuteContext after(ExecuteContext context) throws Exception {
        if (MqGrayscaleConfigUtils.isPlugEnabled()) {
            DefaultMQPullConsumer pullConsumer =
                    ((DefaultMQPullConsumerImpl) context.getObject()).getDefaultMQPullConsumer();
            String baseGroup = pullConsumer.getConsumerGroup();
            String grayGroupTag = MqGrayscaleConfigUtils.getGrayGroupTag();
            if (StringUtils.isEmpty(grayGroupTag)) {
                MqConsumerGroupAutoCheck.setConsumerClientConfig(pullConsumer.getNamesrvAddr(),
                        (String) context.getArguments()[0], baseGroup);
            } else {
                // consumerGroup name rule: ^[%|a-zA-Z0-9_-]+$
                String grayConsumerGroup
                        = baseGroup.contains("_" + grayGroupTag) ? baseGroup : baseGroup + "_" + grayGroupTag;
                pullConsumer.setConsumerGroup(grayConsumerGroup);
                SubscriptionDataUtils.setGrayGroupTagChangeMap(pullConsumer.getNamesrvAddr(),
                        (String) context.getArguments()[0], grayConsumerGroup, true);
            }
        }
        return context;
    }
}
