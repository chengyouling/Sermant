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

import com.huawei.registry.config.GraceConfig;
import com.huawei.registry.support.RegisterSwitchSupport;

import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.plugin.agent.entity.ExecuteContext;
import com.huaweicloud.sermant.core.plugin.config.PluginConfigManager;

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
public class SpringFactoriesVovInterceptor extends RegisterSwitchSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private static final String SPRING_BOOT_AUTOCONFIGURE =
        "org.springframework.boot.autoconfigure.EnableAutoConfiguration";

    private static final String VOLVO_RETRY_AUTOCONFIGURE = "wiki.xsx.core.config.ConsulRetryAutoConfiguration";

    @Override
    public ExecuteContext doAfter(ExecuteContext context) {
        Object result = context.getResult();
        if (result instanceof Map) {
            injectConfigurations((Map<String, List<String>>)result);
        }
        return context;
    }

    private void injectConfigurations(Map<String, List<String>> result) {
        List<String> configurations = result.get(SPRING_BOOT_AUTOCONFIGURE);
        if (configurations != null && configurations.contains(VOLVO_RETRY_AUTOCONFIGURE)) {
            LOGGER.warning("find volvo consul retry class");
            List<String> newConfigurations = new ArrayList<>(configurations);
            newConfigurations.remove(VOLVO_RETRY_AUTOCONFIGURE);
            result.put(SPRING_BOOT_AUTOCONFIGURE, newConfigurations);
        }
    }

    @Override
    protected boolean isEnabled() {
        return registerConfig.isEnableSpringRegister()
                || PluginConfigManager.getPluginConfig(GraceConfig.class).isEnableSpring();
    }
}
