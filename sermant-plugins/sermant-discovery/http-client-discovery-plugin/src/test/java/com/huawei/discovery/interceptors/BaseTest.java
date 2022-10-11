/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.discovery.interceptors;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.huawei.discovery.config.DiscoveryPluginConfig;
import com.huawei.discovery.config.LbConfig;
import com.huawei.discovery.config.PlugEffectWhiteBlackConstants;
import com.huawei.discovery.entity.PlugEffectStategyCache;
import com.huaweicloud.sermant.core.plugin.config.PluginConfigManager;
import com.huaweicloud.sermant.core.plugin.service.PluginServiceManager;
import com.huaweicloud.sermant.core.utils.ReflectUtils;

/**
 * 基础测试
 *
 * @author zhouss
 * @since 2022-10-09
 */
public class BaseTest {
    protected final DiscoveryPluginConfig discoveryPluginConfig = new DiscoveryPluginConfig();

    protected MockedStatic<PluginServiceManager> pluginServiceManagerMockedStatic;

    protected MockedStatic<PluginConfigManager> pluginConfigManagerMockedStatic;

    @Before
    public void setUp() {
        pluginConfigManagerMockedStatic = Mockito
                .mockStatic(PluginConfigManager.class);
        pluginConfigManagerMockedStatic.when(() -> PluginConfigManager.getPluginConfig(DiscoveryPluginConfig.class))
                .thenReturn(discoveryPluginConfig);
        pluginServiceManagerMockedStatic = Mockito
                .mockStatic(PluginServiceManager.class);
        init();
    }

    private void init() {
        final Optional<Object> dynamicConfig = ReflectUtils.getFieldValue(PlugEffectStategyCache.INSTANCE, "caches");
        Assert.assertTrue(dynamicConfig.isPresent() && dynamicConfig.get() instanceof Map);
        ((Map) dynamicConfig.get()).put(PlugEffectWhiteBlackConstants.DYNAMIC_CONFIG_STRATEGY, PlugEffectWhiteBlackConstants.STRATEGY_ALL);
        ((Map) dynamicConfig.get()).put(PlugEffectWhiteBlackConstants.DYNAMIC_CONFIG__VALUE, "service1");
    }

    @After
    public void tearDown() {
        clearCache();
        pluginConfigManagerMockedStatic.close();
        pluginServiceManagerMockedStatic.close();
    }

    private void clearCache() {
        final Optional<Object> dynamicConfig = ReflectUtils.getFieldValue(PlugEffectStategyCache.INSTANCE, "caches");
        Assert.assertTrue(dynamicConfig.isPresent() && dynamicConfig.get() instanceof Map);
        ((Map) dynamicConfig.get()).clear();
    }
}
