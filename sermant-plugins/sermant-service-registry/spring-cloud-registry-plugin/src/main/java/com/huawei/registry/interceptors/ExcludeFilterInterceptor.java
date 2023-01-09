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

package com.huawei.registry.interceptors;

import java.util.Set;

import com.huawei.registry.config.SpecialRemoveConfig;
import com.huaweicloud.sermant.core.plugin.agent.entity.ExecuteContext;
import com.huaweicloud.sermant.core.plugin.agent.interceptor.Interceptor;
import com.huaweicloud.sermant.core.plugin.config.PluginConfigManager;
import com.huaweicloud.sermant.core.utils.ReflectUtils;
import com.huaweicloud.sermant.core.utils.StringUtils;

/**
 * 拦截ClassExcludeFilter注入自定配置源定制化处理
 *
 * @author chengyouling
 * @since 2023-01-06
 */
public class ExcludeFilterInterceptor implements Interceptor {
    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        return context;
    }

    @Override
    public ExecuteContext after(ExecuteContext context) throws Exception {
        Object object = context.getObject();
        Set<String> set = (Set<String>)ReflectUtils.getFieldValue(object, "classNames").get();
        SpecialRemoveConfig config = PluginConfigManager.getPluginConfig(SpecialRemoveConfig.class);
        String componentBean = config.getComponentName();
        if (!StringUtils.isEmpty(componentBean)) {
            String[] names = componentBean.split(",");
            for (String name : names) {
                set.add(name);
            }
        }
        ReflectUtils.setFieldValue(object, "classNames", set);
        return context;
    }

    @Override
    public ExecuteContext onThrow(ExecuteContext context) throws Exception {
        return context;
    }
}
