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

package com.huawei.discovery.service.lb.cache;

import com.huawei.discovery.consul.entity.ServiceInstance;
import com.huawei.discovery.service.config.LbConfig;
import com.huawei.discovery.service.lb.discovery.ServiceDiscoveryClient;

import com.huaweicloud.sermant.core.plugin.config.PluginConfigManager;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 实例缓存
 *
 * @author zhouss
 * @since 2022-09-26
 */
public class InstanceCacheManager {
    private final Cache<String, InstanceCache> cache;

    private final ServiceDiscoveryClient discoveryClient;

    /**
     * 构造器
     *
     * @param discoveryClient 查询客户端
     */
    public InstanceCacheManager(ServiceDiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(PluginConfigManager.getPluginConfig(LbConfig.class).getCacheExpireMs(),
                        TimeUnit.MILLISECONDS)
                .build(new CacheLoader<String, InstanceCache>() {
                    @Override
                    public InstanceCache load(String serviceName) {
                        final Collection<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
                        if (instances != null && !instances.isEmpty()) {
                            return new InstanceCache(serviceName, new ArrayList<>(instances));
                        }
                        return null;
                    }
                });
    }

    /**
     * 获取实例列表
     *
     * @param serviceName 服务名
     * @return 实例列表
     */
    public List<ServiceInstance> getInstances(String serviceName) {
        final Collection<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        final InstanceCache instanceCache = cache.getIfPresent(serviceName);
        if (instanceCache == null) {
            return Collections.emptyList();
        }
        return instanceCache.getInstances();
    }
}
