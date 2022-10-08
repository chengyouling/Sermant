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

package com.huawei.discovery.consul.interceptors;

import com.huawei.discovery.consul.entity.DefaultServiceInstance;
import com.huawei.discovery.consul.entity.ServiceInstance;
import com.huawei.discovery.consul.service.RegistryService;

import com.huaweicloud.sermant.core.plugin.agent.entity.ExecuteContext;
import com.huaweicloud.sermant.core.plugin.agent.interceptor.Interceptor;
import com.huaweicloud.sermant.core.service.ServiceManager;

import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;

import java.util.Map;

/**
 * 拦截获取服务列表
 *
 * @author zhouss
 * @since 2021-12-13
 */
public class RegistrationInterceptor implements Interceptor {
    private final RegistryService registryService;

    /**
     * 构造方法
     */
    public RegistrationInterceptor() {
        registryService = ServiceManager.getService(RegistryService.class);
    }

    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        if (!(context.getArguments()[0] instanceof Registration)) {
            return context;
        }
        String ipAddress = "";
        String instanceZone = "";
//        if (context.getRawMemberFieldValue("properties") instanceof ConsulDiscoveryProperties) {
//            ConsulDiscoveryProperties property = (ConsulDiscoveryProperties)context.getRawMemberFieldValue("properties");
//            ipAddress = property.getIpAddress();
//            instanceZone = property.getInstanceZone();
//        }
        final Registration registration = (Registration) context.getArguments()[0];
        ipAddress = registration.getHost();
        Map<String, String> metadata = registration.getMetadata();
        metadata.putIfAbsent("zone", instanceZone);
        ServiceInstance serviceInstance = new DefaultServiceInstance(registration.getHost(), ipAddress,
                registration.getPort(), metadata, registration.getServiceId());
        registryService.registry(serviceInstance);
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
