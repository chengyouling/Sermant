/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.huawei.registry.interceptors;

import com.huawei.registry.config.SpecialRemoveConfig;

import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.plugin.agent.entity.ExecuteContext;
import com.huaweicloud.sermant.core.plugin.agent.interceptor.Interceptor;
import com.huaweicloud.sermant.core.plugin.config.PluginConfigManager;
import com.huaweicloud.sermant.core.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 定制化处理
 *
 * @author chengyouling
 * @since 2022-12-21
 */
public class SpringFactoriesVovInterceptor implements Interceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private static final String SPRING_BOOT_AUTOCONFIGURE =
        "org.springframework.boot.autoconfigure.EnableAutoConfiguration";

    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        return context;
    }

    @Override
    public ExecuteContext after(ExecuteContext context) throws Exception {
        SpecialRemoveConfig config = PluginConfigManager.getPluginConfig(SpecialRemoveConfig.class);
        String autoConfig = config.getAutoName();
        Object result = context.getResult();
        if (!StringUtils.isEmpty(autoConfig) && result instanceof Map) {
            injectConfigurations((Map<String, List<String>>)result, autoConfig);
        }
        return context;
    }

    private void injectConfigurations(Map<String, List<String>> result, String autoConfig) {
        List<String> configurations = result.get(SPRING_BOOT_AUTOCONFIGURE);
        String[] removeBeans = autoConfig.split(",");
        List<String> newConfigurations = new ArrayList<>(configurations);
        for (String str : removeBeans) {
            if (configurations != null && configurations.contains(str)) {
                LOGGER.warning("find volvo consul retry class: " + str);
                newConfigurations.remove(str);
            }
        }
        result.put(SPRING_BOOT_AUTOCONFIGURE, newConfigurations);
    }

    @Override
    public ExecuteContext onThrow(ExecuteContext context) throws Exception {
        return context;
    }
}
