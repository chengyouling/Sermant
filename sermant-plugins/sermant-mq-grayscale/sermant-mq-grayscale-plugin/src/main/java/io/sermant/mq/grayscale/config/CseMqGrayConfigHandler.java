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

import io.sermant.core.common.LoggerFactory;
import io.sermant.core.service.dynamicconfig.common.DynamicConfigEvent;
import io.sermant.core.service.dynamicconfig.common.DynamicConfigEventType;
import io.sermant.core.utils.StringUtils;
import io.sermant.mq.grayscale.utils.MqGrayscaleConfigUtils;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * grayscale dynamic config handler
 *
 * @author chengyouling
 * @since 2024-05-27
 **/
public class CseMqGrayConfigHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    protected final Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));

    public void handle(DynamicConfigEvent event) {
        if ("grayscale.mq.config".equals(event.getKey())) {
            if (event.getEventType() == DynamicConfigEventType.DELETE) {
                MqGrayscaleConfigUtils.resetGrayscaleConfig();
                return;
            }
            if (!StringUtils.isEmpty(event.getContent())) {
                LOGGER.warning(String.format(Locale.ROOT,
                        "mqGrayscale dynamicConfig current context: %s", event.getContent()));
                Map<String, Object> map = yaml.load(event.getContent());
                MqGrayscaleConfig config = buildGrayConfig(map);
                MqGrayscaleConfigUtils.setGrayscaleConfig(config);
            } else {
                MqGrayscaleConfigUtils.setGrayscaleConfig(new MqGrayscaleConfig());
            }
        }
    }

    private MqGrayscaleConfig buildGrayConfig(Map<String, Object> map) {
        MqGrayscaleConfig config = new MqGrayscaleConfig();
        config.setEnabled(Boolean.parseBoolean(map.get("enabled").toString()));
        config.setServerGrayEnabled(Boolean.parseBoolean(map.get("serverGrayEnabled").toString()));
        BaseMessage base = new BaseMessage();
        base.setConsumeType((String) map.get("base.consumeType"));
        base.setAutoCheckDelayTime(Long.parseLong(map.get("base.autoCheckDelayTime").toString()));
        List<GrayTagItem> grayTagItems = new ArrayList<>();
        if (map.get("grayscale") != null) {
            List list = (List) map.get("grayscale");
            for (Object object : list) {
                Map<String, Object> itemMap = (Map<String, Object>) object;
                GrayTagItem item = new GrayTagItem();
                item.setConsumerGroupTag((String) itemMap.get("consumerGroupTag"));
                item.setEnvTag((Map<String, String>) itemMap.get("envTag"));
                item.setTrafficTag((Map<String, String>) itemMap.get("trafficTag"));
                grayTagItems.add(item);
            }
        }
        config.setGrayscale(grayTagItems);
        config.setBase(base);
        return config;
    }
}
