/*
 * Copyright (C) 2021-2022 Huawei Technologies Co., Ltd. All rights reserved.
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

package io.sermant.dubbo.registry.alibaba;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.support.FailbackRegistry;

import io.sermant.core.plugin.service.PluginServiceManager;
import io.sermant.dubbo.registry.service.RegistryService;

/**
 * SC Registration
 *
 * @author provenceee
 * @since 2021-12-15
 */
public class ServiceCenterRegistry extends FailbackRegistry {
    private final RegistryService registryService;

    /**
     * Constructor
     *
     * @param url Registration URL
     */
    public ServiceCenterRegistry(com.alibaba.dubbo.common.URL url) {
        super(url);
        registryService = PluginServiceManager.getPluginService(RegistryService.class);
    }

    @Override
    public void doRegister(com.alibaba.dubbo.common.URL url) {
        registryService.addRegistryUrls(url);
    }

    @Override
    public void doUnregister(com.alibaba.dubbo.common.URL url) {
        registryService.shutdown();
    }

    @Override
    public void doSubscribe(com.alibaba.dubbo.common.URL url, NotifyListener notifyListener) {
        registryService.doSubscribe(url, notifyListener);
    }

    @Override
    public void doUnsubscribe(com.alibaba.dubbo.common.URL url, NotifyListener notifyListener) {
        registryService.shutdown();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * 判断是否已注册
     *
     * @param url
     * @return 服务是否注册
     */
    public boolean exists(URL url) {
        return true;
    }
}
