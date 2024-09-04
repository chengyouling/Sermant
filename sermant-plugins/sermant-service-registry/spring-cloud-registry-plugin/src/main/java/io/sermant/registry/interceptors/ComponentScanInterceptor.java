/*
 * Copyright (C) 2023-2023 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sermant.registry.interceptors;

import io.sermant.core.common.LoggerFactory;
import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.core.plugin.agent.interceptor.Interceptor;
import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.registry.config.SpecialRemoveConfig;

import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.logging.Logger;

/**
 * 拦截ComponentScan注入自定配置源定制化处理
 *
 * @author chengyouling
 * @since 2023-01-06
 */
public class ComponentScanInterceptor implements Interceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public ExecuteContext before(ExecuteContext context) {
        ClassPathBeanDefinitionScanner scanner = (ClassPathBeanDefinitionScanner) context.getObject();
        SpecialRemoveConfig config = PluginConfigManager.getPluginConfig(SpecialRemoveConfig.class);
        String[] componentBean = config.getComponentName().split(",");
        for (String className : componentBean) {
            try {
                Class<?> clazz = Class.forName(className);
                scanner.addExcludeFilter(new AssignableTypeFilter(clazz));
            } catch (ClassNotFoundException e) {
                LOGGER.warning("ComponentScanInterceptor can not find class: " + className);
            }
        }
        return context;
    }

    @Override
    public ExecuteContext after(ExecuteContext context) throws Exception {
        return context;
    }

    @Override
    public ExecuteContext onThrow(ExecuteContext context) throws Exception {
        return context;
    }
}
