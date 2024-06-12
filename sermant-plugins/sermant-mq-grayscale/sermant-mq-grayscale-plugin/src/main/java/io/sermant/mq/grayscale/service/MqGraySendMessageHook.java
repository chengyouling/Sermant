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

import io.sermant.core.utils.StringUtils;
import io.sermant.mq.grayscale.utils.MqGrayscaleConfigUtils;

import org.apache.rocketmq.client.hook.SendMessageContext;
import org.apache.rocketmq.client.hook.SendMessageHook;
import org.apache.rocketmq.common.message.Message;

public class MqGraySendMessageHook implements SendMessageHook {
    @Override
    public String hookName() {
        return "MqGraySendMessageHook";
    }

    @Override
    public void sendMessageBefore(SendMessageContext context) {
        Message message = context.getMessage();
        String grayTag = MqGrayscaleConfigUtils.getGrayEnvTag();
        // 如果是灰度环境，设置灰度peoperty
        if (!StringUtils.isEmpty(grayTag)) {
            message.putUserProperty(MqGrayscaleConfigUtils.MICRO_SERVICE_GRAY_TAG_KEY, grayTag);
        } else {
            // 如果不存在环境灰度标签，查看流量标签
            String grayTrafficTag = MqGrayscaleConfigUtils.getTrafficGrayTag();
            if (!StringUtils.isEmpty(grayTrafficTag)) {
                message.putUserProperty(MqGrayscaleConfigUtils.MICRO_TRAFFIC_GRAY_TAG_KEY, grayTrafficTag);
            }
        }
    }

    @Override
    public void sendMessageAfter(SendMessageContext context) {
    }
}
