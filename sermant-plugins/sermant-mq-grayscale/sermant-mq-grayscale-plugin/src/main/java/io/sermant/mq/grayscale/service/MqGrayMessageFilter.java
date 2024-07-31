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
import io.sermant.mq.grayscale.config.GrayTagItem;
import io.sermant.mq.grayscale.config.MqGrayscaleConfig;
import io.sermant.mq.grayscale.utils.MqGrayscaleConfigUtils;
import io.sermant.mq.grayscale.utils.SubscriptionDataUtils;

import org.apache.rocketmq.client.hook.FilterMessageContext;
import org.apache.rocketmq.client.hook.FilterMessageHook;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * gray message filter service
 *
 * @author chengyouling
 * @since 2024-05-27
 **/
public class MqGrayMessageFilter implements FilterMessageHook {
    private static final String CONSUME_TYPE_ALL = "all";

    private static final String CONSUME_TYPE_BASE = "base";

    @Override
    public String hookName() {
        return "MqGrayMessageFilter";
    }

    @Override
    public void filterMessage(FilterMessageContext context) {
        String grayGroupTag = MqGrayscaleConfigUtils.getGrayGroupTag();
        List<MessageExt> MessageExts = context.getMsgList();
        MqGrayscaleConfig configs = MqGrayscaleConfigUtils.getGrayscaleConfigs();
        if (configs == null) {
            return;
        }
        Iterator<MessageExt> iterator = MessageExts.iterator();
        while (iterator.hasNext()) {
            MessageExt message = iterator.next();
            if (StringUtils.isEmpty(grayGroupTag)) {
                filterBasicMessage(iterator, message, configs.getGrayscale());
            } else {
                filterGrayscaleMessage(iterator, message, configs, grayGroupTag);
            }
        }
    }

    private void filterGrayscaleMessage(Iterator<MessageExt> iterator, MessageExt message, MqGrayscaleConfig config,
        String grayGroupTag) {
        GrayTagItem item = MqGrayscaleConfigUtils.getScaleByGroupTag(grayGroupTag, config.getGrayscale());
        if (item == null) {
            return;
        }
        Set<String> set = new HashSet<>();
        if (!item.getEnvTag().isEmpty()) {
            set.addAll(item.getEnvTag().keySet());
        }
        if (!item.getTrafficTag().isEmpty()) {
            set.addAll(item.getTrafficTag().keySet());
        }
        for (String grayTagKey : set) {
            if (message.getUserProperty(grayTagKey) != null) {
                return;
            }
        }
        iterator.remove();
    }

    private void filterBasicMessage(Iterator<MessageExt> iterator, MessageExt message, List<GrayTagItem> items) {
        if (CONSUME_TYPE_ALL.equals(MqGrayscaleConfigUtils.getConsumeType())) {
            return;
        }
        if (CONSUME_TYPE_BASE.equals(MqGrayscaleConfigUtils.getConsumeType())) {
            if (!items.isEmpty()) {
                for (String grayTagKey : MqGrayscaleConfigUtils.buildGrayTagKeySet(items)) {
                    if (message.getUserProperty(grayTagKey) != null) {
                        iterator.remove();
                        return;
                    }
                }
            }
        }
        if (SubscriptionDataUtils.getGrayTags() != null) {
            filterBaseAutoMessage(iterator, message, SubscriptionDataUtils.getGrayTags());
        }
    }

    private void filterBaseAutoMessage(Iterator<MessageExt> iterator, MessageExt message, List<GrayTagItem> items) {
        Set<String> set = MqGrayscaleConfigUtils.buildGrayTagKeySet(items);
        for (String grayTagKey : set) {
            if (message.getUserProperty(grayTagKey) != null) {
                iterator.remove();
                return;
            }
        }
    }
}
