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

import com.huawei.discovery.consul.config.LbConfig;
import com.huawei.discovery.consul.entity.DefaultServiceInstance;
import com.huawei.discovery.consul.entity.ServiceInstance;
import com.huawei.discovery.consul.entity.ServiceInstance.Status;
import com.huawei.discovery.service.lb.discovery.ServiceDiscoveryClient;

import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.plugin.config.PluginConfigManager;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryForever;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceType;
import org.apache.curator.x.discovery.UriSpec;
import org.apache.curator.x.discovery.details.InstanceSerializer;
import org.springframework.cloud.zookeeper.discovery.ZookeeperInstance;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
public class ZkDiscoveryClient implements ServiceDiscoveryClient {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private static final String STATUS_KEY = "instance_status";

    private final LbConfig lbConfig;

    private CuratorFramework curatorFramework;

    private ServiceDiscovery<ZookeeperInstance> serviceDiscovery;

    private org.apache.curator.x.discovery.ServiceInstance<ZookeeperInstance> instance;

    /**
     * zk客户端
     */
    public ZkDiscoveryClient() {
        this.lbConfig = PluginConfigManager.getPluginConfig(LbConfig.class);
    }

    @Override
    public void init() {
        this.curatorFramework = buildClient();
        this.curatorFramework.start();
        this.serviceDiscovery = build();
        try {
            this.serviceDiscovery.start();
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Can not start zookeeper discovery client!", exception);
        }
    }

    @Override
    public boolean registry(ServiceInstance serviceInstance) {
        final String id = UUID.randomUUID().toString();
        final HashMap<String, String> metadata = new HashMap<>(serviceInstance.getMetadata());
        metadata.put("sermant-discovery", "zk");
        final ZookeeperInstance zookeeperServiceInstance =
                new ZookeeperInstance(getAddress(serviceInstance) + ":" + serviceInstance.getPort(),
                        serviceInstance.serviceName(), metadata);
        instance = new org.apache.curator.x.discovery.ServiceInstance<>(
                serviceInstance.serviceName(), id,
                getAddress(serviceInstance),
                serviceInstance.getPort(),
                null, zookeeperServiceInstance, System.currentTimeMillis(), ServiceType.DYNAMIC,
                new UriSpec(lbConfig.getZkUriSpec()));
        try {
            this.serviceDiscovery.registerService(instance);
            return true;
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Can not register service to zookeeper", exception);
        }
        return false;
    }

    private String getAddress(ServiceInstance serviceInstance) {
        return lbConfig.isPreferIpAddress() ? serviceInstance.getIp() : serviceInstance.getHost();
    }

    @Override
    public Collection<ServiceInstance> getInstances(String serviceId) {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(ZkDiscoveryClient.class.getClassLoader());
            return convert(serviceDiscovery.queryForInstances(serviceId));
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Can not query service instances from registry center!", exception);
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
        return Collections.emptyList();
    }

    private Collection<ServiceInstance> convert(
            Collection<org.apache.curator.x.discovery.ServiceInstance<ZookeeperInstance>> serviceInstances) {
        if (serviceInstances == null || serviceInstances.isEmpty()) {
            return Collections.emptyList();
        }
        return serviceInstances.stream().map(this::convert).collect(Collectors.toList());
    }

    private ServiceInstance convert(org.apache.curator.x.discovery.ServiceInstance<ZookeeperInstance> curInstance) {
        final DefaultServiceInstance serviceInstance = new DefaultServiceInstance();
        serviceInstance.setHost(curInstance.getAddress());
        serviceInstance.setIp(curInstance.getAddress());
        serviceInstance.setServiceName(curInstance.getName());
        serviceInstance.setPort(curInstance.getPort());
        if (curInstance.getPayload() != null) {
            final ZookeeperInstance payload = curInstance.getPayload();
            serviceInstance.setMetadata(payload.getMetadata());
            serviceInstance.setStatus(payload.getMetadata().getOrDefault(STATUS_KEY, Status.UP.name()));
        }
        serviceInstance.setId(curInstance.getAddress() + ":" + curInstance.getPort());
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

    @Override
    public boolean unRegistry() {
        if (instance != null) {
            try {
                this.serviceDiscovery.unregisterService(instance);
                return true;
            } catch (Exception exception) {
                LOGGER.log(Level.WARNING, "Can not un registry from zookeeper center!", exception);
            }
        }
        return false;
    }

    private ServiceDiscovery<ZookeeperInstance> build() {
        return ServiceDiscoveryBuilder.builder(ZookeeperInstance.class)
                .client(this.curatorFramework)
                .basePath(lbConfig.getZkBasePath())
                .serializer(new ZkInstanceSerializer<>(ZookeeperInstance.class))
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
        try {
            this.serviceDiscovery.close();
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Stop zookeeper discovery client failed", ex);
        }
    }

    /**
     * 自定义zk序列化器, 而非使用原生的{@link org.apache.curator.x.discovery.details.JsonInstanceSerializer} 避免漏洞问题, 替换ObjectMapper
     *
     * @param <T> 实例类型
     * @since 2022-10-08
     */
    static class ZkInstanceSerializer<T> implements InstanceSerializer<T> {
        private final ObjectMapper mapper;
        private final Class<T> payloadClass;
        private final JavaType type;

        ZkInstanceSerializer(Class<T> payloadClass) {
            this.payloadClass = payloadClass;
            mapper = new ObjectMapper();
            type = mapper.getTypeFactory().constructType(WriteAbleServiceInstance.class);
        }

        @Override
        public org.apache.curator.x.discovery.ServiceInstance<T> deserialize(byte[] bytes) throws Exception {
            WriteAbleServiceInstance<T> rawServiceInstance = mapper.readValue(bytes, type);
            payloadClass.cast(rawServiceInstance.getPayload());
            return rawServiceInstance;
        }

        @Override
        public byte[] serialize(org.apache.curator.x.discovery.ServiceInstance<T> instance) throws Exception {
            return mapper.writeValueAsBytes(new WriteAbleServiceInstance<>(instance));
        }
    }

    /**
     * 序列化的实例, 标记payload类型, 便于与其他开源的zk数据可互相识别
     *
     * @param <T> 实例类型
     * @since 2022-10-08
     */
    public static class WriteAbleServiceInstance<T> extends org.apache.curator.x.discovery.ServiceInstance<T> {
        /**
         * 构造器
         *
         * @param instance 实例信息
         */
        public WriteAbleServiceInstance(org.apache.curator.x.discovery.ServiceInstance<T> instance) {
            super(instance.getName(), instance.getId(), instance.getAddress(), instance.getPort(),
                    instance.getSslPort(), instance.getPayload(),
                    instance.getRegistrationTimeUTC(),
                    instance.getServiceType(),
                    instance.getUriSpec());
        }

        WriteAbleServiceInstance() {
            super("", "", null, null, null, null, 0, ServiceType.DYNAMIC, null, true);
        }

        @Override
        @JsonTypeInfo(use = Id.CLASS, defaultImpl = Object.class)
        public T getPayload() {
            return super.getPayload();
        }
    }
}
