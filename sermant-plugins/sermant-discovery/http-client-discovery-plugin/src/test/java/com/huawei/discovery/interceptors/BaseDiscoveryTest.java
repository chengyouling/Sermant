/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.discovery.interceptors;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.huaweicloud.sermant.core.plugin.agent.entity.ExecuteContext;
import com.huaweicloud.sermant.core.plugin.agent.interceptor.Interceptor;
import com.huaweicloud.sermant.core.plugin.config.PluginConfigManager;
import com.huaweicloud.sermant.core.plugin.service.PluginServiceManager;
import com.huaweicloud.sermant.core.service.ServiceManager;

/**
 * 测试基础配置
 *
 * @author zhouss
 * @since 2022-09-07
 */
public abstract class BaseDiscoveryTest<T extends Interceptor> {
//    protected static final RealmNameConfig REALM_NAME_CONFIG = new RealmNameConfig();

    protected static MockedStatic<PluginConfigManager> pluginConfigManagerMockedStatic;

    protected static MockedStatic<PluginServiceManager> pluginServiceManagerMockedStatic;

    protected static MockedStatic<ServiceManager> serviceManagerMockedStatic;

    protected T interceptor;

    protected BaseDiscoveryTest() {
        interceptor = getInterceptor();
    }

    @BeforeClass
    public static void init() {
        pluginConfigManagerMockedStatic = Mockito.mockStatic(PluginConfigManager.class);
//        pluginConfigManagerMockedStatic.when(() -> PluginConfigManager.getPluginConfig(RealmNameConfig.class))
//                .thenReturn(REALM_NAME_CONFIG);
        pluginServiceManagerMockedStatic = Mockito.mockStatic(PluginServiceManager.class);
        serviceManagerMockedStatic = Mockito.mockStatic(ServiceManager.class);

    }

    @AfterClass
    public static void clear() {
        pluginConfigManagerMockedStatic.close();
        pluginServiceManagerMockedStatic.close();
        serviceManagerMockedStatic.close();
    }

    /**
     *
     * @return 测试拦截器
     */
    protected abstract T getInterceptor();

    /**
     * 构建基本的context
     *
     * @return context
     * @throws NoSuchMethodException 不会抛出
     */
    protected ExecuteContext buildContext() throws NoSuchMethodException {
        return buildContext(this, null);
    }

    /**
     * 构建基本的context
     *
     * @param arguments 参数
     * @param target  对象
     * @return context
     * @throws NoSuchMethodException 不会抛出
     */
    protected ExecuteContext buildContext(Object target, Object[] arguments) throws NoSuchMethodException {
        return ExecuteContext.forMemberMethod(target, String.class.getDeclaredMethod("trim"),
                arguments, null, null);
    }
}
