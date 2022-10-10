/*
 * Copyright (C) 2021-2021 Huawei Technologies Co., Ltd. All rights reserved.
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

import org.springframework.cloud.client.serviceregistry.Registration;

import com.huawei.discovery.entity.DefaultServiceInstance;
import com.huawei.discovery.entity.RegisterContext;
import com.huawei.discovery.entity.ServiceInstance;
import com.huawei.discovery.service.ConfigCenterService;
import com.huawei.discovery.service.RegistryService;
import com.huawei.discovery.utils.HostIpAddressUtils;
import com.huaweicloud.sermant.core.config.ConfigManager;
import com.huaweicloud.sermant.core.plugin.agent.entity.ExecuteContext;
import com.huaweicloud.sermant.core.plugin.agent.interceptor.Interceptor;
import com.huaweicloud.sermant.core.plugin.config.ServiceMeta;
import com.huaweicloud.sermant.core.service.ServiceManager;
import com.huaweicloud.sermant.core.utils.StringUtils;

/**
 * 拦截获取服务列表
 *
 * @author zhouss
 * @since 2021-12-13
 */
public class RegistrationInterceptor implements Interceptor {

    private final ConfigCenterService configCenterService;

    private static String SERVICE_NAME = "";

    /**
     * 构造方法
     */
    public RegistrationInterceptor() {
        configCenterService = ServiceManager.getService(ConfigCenterService.class);
    }

    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        if (!(context.getArguments()[0] instanceof Registration)) {
            return context;
        }
        final Registration registration = (Registration) context.getArguments()[0];
        SERVICE_NAME = registration.getServiceId();
        Map<String, String> metadata = registration.getMetadata();
        ServiceMeta serviceMeta = ConfigManager.getConfig(ServiceMeta.class);
        metadata.putIfAbsent("zone", serviceMeta.getZone());
        RegisterContext.INSTANCE.getServiceInstance().setMetadata(metadata);
        RegisterContext.INSTANCE.getServiceInstance().setHost(registration.getHost());
        RegisterContext.INSTANCE.getServiceInstance().setPort(registration.getPort());
        RegisterContext.INSTANCE.getServiceInstance().setServiceName(registration.getServiceId());
        return context;
    }

    @Override
    public ExecuteContext after(ExecuteContext context) throws Exception {
        configCenterService.init(SERVICE_NAME);
        return context;
    }

    @Override
    public ExecuteContext onThrow(ExecuteContext context) throws Exception {
        return context;
    }
}
