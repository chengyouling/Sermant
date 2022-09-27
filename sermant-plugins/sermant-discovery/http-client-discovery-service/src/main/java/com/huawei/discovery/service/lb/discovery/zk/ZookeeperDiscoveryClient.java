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

package com.huawei.discovery.service.lb.discovery.zk;

import com.huawei.discovery.consul.entity.ServiceInstance;
import com.huawei.discovery.service.config.LbConfig;
import com.huawei.discovery.service.lb.discovery.ServiceDiscoveryClient;
import com.huawei.discovery.service.lb.discovery.entity.DefaultServiceInstance;

import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.plugin.config.PluginConfigManager;
import com.huaweicloud.sermant.core.utils.ReflectUtils;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryForever;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceType;
import org.apache.curator.x.discovery.details.InstanceSerializer;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * zookeeper实现
 *
 * @author zhouss
 * @since 2022-09-26
 */
public class ZookeeperDiscoveryClient implements ServiceDiscoveryClient {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private final LbConfig lbConfig;

    private CuratorFramework curatorFramework;

    private ServiceDiscovery<Object> serviceDiscovery;

    /**
     * zk客户端
     */
    public ZookeeperDiscoveryClient() {
        this.lbConfig = PluginConfigManager.getPluginConfig(LbConfig.class);
    }

    @Override
    public void init() {
        this.curatorFramework = buildClient();
        this.curatorFramework.start();
        this.serviceDiscovery = build();
    }

    @Override
    public boolean registry(ServiceInstance serviceInstance) {
        final String id = UUID.randomUUID().toString();
        final ZookeeperServiceInstance zookeeperServiceInstance = new ZookeeperServiceInstance(id,
                serviceInstance.serviceName(), serviceInstance.getMetadata());
        final org.apache.curator.x.discovery.ServiceInstance<Object> instance =
                new org.apache.curator.x.discovery.ServiceInstance<>(
                        serviceInstance.serviceName(), id, serviceInstance.getHost(), serviceInstance.getPort(),
                        0, zookeeperServiceInstance, System.currentTimeMillis(), ServiceType.DYNAMIC, null);
        try {
            this.serviceDiscovery.registerService(instance);
            return true;
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Can not register service to zookeeper", exception);
        }
        return false;
    }

    @Override
    public Collection<ServiceInstance> getInstances(String serviceId) {
        try {
            return convert(serviceDiscovery.queryForInstances(serviceId));
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Can not query service instances from registry center!", exception);
        }
        return Collections.emptyList();
    }

    private Collection<ServiceInstance> convert(
            Collection<org.apache.curator.x.discovery.ServiceInstance<Object>> serviceInstances) {
        if (serviceInstances == null || serviceInstances.isEmpty()) {
            return Collections.emptyList();
        }
        return serviceInstances.stream().map(this::convert).collect(Collectors.toList());
    }

    private ServiceInstance convert(org.apache.curator.x.discovery.ServiceInstance<Object> instance) {
        final DefaultServiceInstance serviceInstance = new DefaultServiceInstance();
        serviceInstance.setHost(instance.getAddress());
        serviceInstance.setIp(instance.getAddress());
        serviceInstance.setServiceName(instance.getName());
        serviceInstance.setPort(instance.getPort());
        if (instance.getPayload() != null) {
            final Optional<Object> metadata = ReflectUtils.getFieldValue(instance.getPayload(), "metadata");
            if (metadata.isPresent() && metadata.get() instanceof Map) {
                serviceInstance.setMetadata((Map<String, String>) metadata.get());
            }
        }
        return serviceInstance;
    }

    @Override
    public Collection<String> getServices() {
        try {
            return serviceDiscovery.queryForNames();
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Can not query services from registry center!", exception);
        }
        return Collections.emptyList();
    }

    private ServiceDiscovery<Object> build() {
        return ServiceDiscoveryBuilder.builder(Object.class)
                .client(this.curatorFramework)
                .basePath(lbConfig.getZkBasePath())
                .serializer(new ZkJsonInstanceSerializer<>())
                .watchInstances(false)
                .build();
    }

    private CuratorFramework buildClient() {
        return CuratorFrameworkFactory.newClient(lbConfig.getRegistryAddress(), lbConfig.getReadTimeoutMs(),
                lbConfig.getConnectionTimeoutMs(), new RetryForever(lbConfig.getRetryIntervalMs()));
    }

    @Override
    public void close() {
        curatorFramework.close();
    }

    /**
     * 序列化器
     *
     * @param <T> 实例类型
     */
    static class ZkJsonInstanceSerializer<T> implements InstanceSerializer<T> {
        private final ObjectMapper mapper;
        private final JavaType type;

        /**
         * 实例
         */
        ZkJsonInstanceSerializer() {
            this.mapper = new ObjectMapper();
            this.type = mapper.getTypeFactory().constructType(org.apache.curator.x.discovery.ServiceInstance.class);
        }

        @Override
        public byte[] serialize(org.apache.curator.x.discovery.ServiceInstance<T> instance) throws Exception {
            return mapper.writeValueAsBytes(instance);
        }

        @Override
        public org.apache.curator.x.discovery.ServiceInstance<T> deserialize(byte[] bytes) throws Exception {
            return mapper.readValue(bytes, type);
        }
    }
}
