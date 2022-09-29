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

package com.huawei.discovery.service.lb;

import com.huawei.discovery.consul.config.LbConfig;
import com.huawei.discovery.consul.entity.ServiceInstance;
import com.huawei.discovery.service.lb.cache.InstanceCacheManager;
import com.huawei.discovery.service.lb.discovery.ServiceDiscoveryClient;
import com.huawei.discovery.service.lb.discovery.zk.ZookeeperDiscoveryClient;
import com.huawei.discovery.service.lb.filter.InstanceFilter;
import com.huawei.discovery.service.lb.rule.AbstractLoadbalancer;
import com.huawei.discovery.service.lb.rule.Loadbalancer;
import com.huawei.discovery.service.lb.rule.RoundRobinLoadbalancer;

import com.huaweicloud.sermant.core.plugin.config.PluginConfigManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * 负载均衡管理器
 *
 * @author zhouss
 * @since 2022-09-26
 */
public enum DiscoveryManager {
    /**
     * 单例
     */
    INSTANCE;

    private final Map<String, AbstractLoadbalancer> lbCache = new HashMap<>();

    private final List<InstanceFilter> filters = new ArrayList<>();

    private final AbstractLoadbalancer defaultLb = new RoundRobinLoadbalancer();

    private LbConfig lbConfig;

    private ServiceDiscoveryClient serviceDiscoveryClient;

    private InstanceCacheManager cacheManager;

    private void initServiceDiscoveryClient() {
        serviceDiscoveryClient = new ZookeeperDiscoveryClient();
        serviceDiscoveryClient.init();
    }

    private void loadLb() {
        for (AbstractLoadbalancer loadbalancer : ServiceLoader.load(AbstractLoadbalancer.class, this.getClass()
                .getClassLoader())) {
            lbCache.put(loadbalancer.lbType(), loadbalancer);
        }
    }

    /**
     * 注册
     *
     * @param serviceInstance 注册实例
     */
    public void registry(ServiceInstance serviceInstance) {
        serviceDiscoveryClient.registry(serviceInstance);
    }

    /**
     * 启动方法
     */
    public void start() {
        initServiceDiscoveryClient();
        loadLb();
        loadFilter();
        lbConfig = PluginConfigManager.getPluginConfig(LbConfig.class);
        cacheManager = new InstanceCacheManager(serviceDiscoveryClient);
    }

    private void loadFilter() {
        for (InstanceFilter filter : ServiceLoader.load(InstanceFilter.class, this.getClass()
                .getClassLoader())) {
            this.filters.add(filter);
        }
    }

    /**
     * 停止相关服务
     *
     * @throws IOException 停止失败抛出
     */
    public void stop() throws IOException {
        serviceDiscoveryClient.unRegistry();
        serviceDiscoveryClient.close();
    }

    /**
     * 基于缓存以及负载均衡规则选择实例
     *
     * @param serviceName 服务名
     * @return ServiceInstance
     */
    public Optional<ServiceInstance> choose(String serviceName) {
        return choose(serviceName, getLoadbalancer());
    }

    /**
     * 选择实例
     *
     * @param serviceName 服务名
     * @param lb 负载均衡
     * @return ServiceInstance
     */
    public Optional<ServiceInstance> choose(String serviceName, Loadbalancer lb) {
        List<ServiceInstance> instances = cacheManager.getInstances(serviceName);
        for (InstanceFilter filter : filters) {
            instances = filter.filter(instances);
        }
        return lb.choose(serviceName, instances);
    }

    private Loadbalancer getLoadbalancer() {
        return lbCache.getOrDefault(lbConfig.getLbType(), defaultLb);
    }
}
