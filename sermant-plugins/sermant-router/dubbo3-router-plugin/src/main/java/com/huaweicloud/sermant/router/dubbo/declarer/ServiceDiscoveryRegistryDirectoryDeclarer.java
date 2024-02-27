/*
 * Copyright (C) 2024-2024 Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huaweicloud.sermant.router.dubbo.declarer;

import com.huaweicloud.sermant.core.plugin.agent.matcher.MethodMatcher;
import com.huaweicloud.sermant.router.common.declarer.AbstractDeclarer;

/**
 * dubbo3.x instance注册类型，增强ServiceDiscoveryRegistryDirectory类的overrideWithConfigurator方法
 *
 * @author chengyouling
 * @since 2024-02-17
 */
public class ServiceDiscoveryRegistryDirectoryDeclarer extends AbstractDeclarer {
    private static final String[] ENHANCE_CLASS
            = {"org.apache.dubbo.registry.client.ServiceDiscoveryRegistryDirectory"};

    private static final String INTERCEPT_CLASS
            = "com.huaweicloud.sermant.router.dubbo.interceptor.ServiceDiscoveryRegistryDirectoryInterceptor";

    private static final String METHOD_NAME = "overrideWithConfigurator";

    /**
     * 构造方法
     */
    public ServiceDiscoveryRegistryDirectoryDeclarer() {
        super(ENHANCE_CLASS, INTERCEPT_CLASS, METHOD_NAME);
    }

    @Override
    public MethodMatcher getMethodMatcher() {
        return super.getMethodMatcher();
    }
}
