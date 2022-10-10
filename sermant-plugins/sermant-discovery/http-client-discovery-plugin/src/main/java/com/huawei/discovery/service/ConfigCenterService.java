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

package com.huawei.discovery.service;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.huawei.discovery.config.RealmServiceDynamicConfigListener;
import com.huawei.discovery.config.SealmServiceMappingConfig;
import com.huawei.discovery.entity.RealmServerNameCache;
import com.huawei.discovery.factory.RealmServiceThreadFactory;

import com.huaweicloud.sermant.core.plugin.config.PluginConfigManager;
import com.huaweicloud.sermant.core.plugin.service.PluginService;
import com.huaweicloud.sermant.core.plugin.subscribe.ConfigSubscriber;
import com.huaweicloud.sermant.core.plugin.subscribe.DefaultGroupConfigSubscriber;
import com.huaweicloud.sermant.core.service.ServiceManager;
import com.huaweicloud.sermant.core.service.dynamicconfig.DynamicConfigService;

/**
 * 域名、服务名映射动态配置服务
 *
 * @author zhouss
 * @since 2022-01-25
 */
public class ConfigCenterService implements PluginService {

    @Override
    public void stop() {
        RealmServerNameCache.INSTANCE.release();
    }

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0,
            TimeUnit.SECONDS, new SynchronousQueue<>(), new RealmServiceThreadFactory("REALM_SERVICE_MAPPING_INIT_THREAD"));

    private final RealmServiceLifeCycle realmServiceLifeCycle = new RealmServiceLifeCycle();

    /**
     * 启动初始化任务 此处脱离service生命周期，通过拦截点控制，以便获取准确数据
     */
    public void doStart() {
        executor.execute(realmServiceLifeCycle);
    }

    /**
     * 域名、服务名映射初始化逻辑生命周期
     *
     * @since 2022-03-22
     */
    static class RealmServiceLifeCycle implements Runnable {

        @Override
        public void run() {
            final SealmServiceMappingConfig pluginConfig = PluginConfigManager.getPluginConfig(SealmServiceMappingConfig.class);
            if (pluginConfig.isUseDynamicConfig()) {
                DynamicConfigService dynamicConfigService = ServiceManager.getService(DynamicConfigService.class);
                ConfigSubscriber configSubscriber = new DefaultGroupConfigSubscriber(
                        pluginConfig.getServiceName(),new RealmServiceDynamicConfigListener(), dynamicConfigService,
                        "RealnServiceMapping");
                configSubscriber.subscribe();
            }
        }
    }
}
