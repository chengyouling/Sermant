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

package com.huawei.discovery.service.lb.discovery.entity;

import com.huawei.discovery.consul.entity.InstanceStats;
import com.huawei.discovery.consul.entity.ServiceInstance;

import java.util.Map;

/**
 * 服务实例默认实现
 *
 * @author zhouss
 * @since 2022-09-26
 */
public class DefaultServiceInstance implements ServiceInstance {
    private InstanceStats instanceStats;

    private String serviceName;

    private String host;

    private String ip;

    private int port;

    private Map<String, String> metadata;

    /**
     * 默认构造器
     */
    public DefaultServiceInstance() {
    }

    /**
     * 构造器
     *
     * @param host 域名
     * @param ip ip
     * @param port 端口
     * @param metadata 元数据
     * @param serviceName 服务名
     */
    public DefaultServiceInstance(String host, String ip, int port,
            Map<String, String> metadata, String serviceName) {
        this.host = host;
        this.ip = ip;
        this.port = port;
        this.metadata = metadata;
        this.serviceName = serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    @Override
    public String serviceName() {
        return serviceName;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getIp() {
        return ip;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public InstanceStats getStats() {
        if (instanceStats == null) {
            instanceStats = new InstanceStats();
        }
        return instanceStats;
    }

    public void setInstanceStats(InstanceStats instanceStats) {
        this.instanceStats = instanceStats;
    }
}
