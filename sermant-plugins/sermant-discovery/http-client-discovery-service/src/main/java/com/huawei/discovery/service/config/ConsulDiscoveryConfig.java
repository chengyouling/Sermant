/*
 * Copyright (C) 2021-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.huawei.discovery.service.config;

import com.huaweicloud.sermant.core.config.common.ConfigTypeKey;
import com.huaweicloud.sermant.core.plugin.config.PluginConfig;

/**
 * consul中心的相关配置
 *
 * @author chengyouling
 * @since 2022-09-21
 */
@ConfigTypeKey("consul.discovery.plugin")
public class ConsulDiscoveryConfig implements PluginConfig {

    /**
     * consul服务地址
     */
    private String host = "localhost";

    /**
     * 调用协议http、https
     */
    private String scheme = "http";

    /**
     * consul服务端口号
     */
    private int port = 8500;

    /**
     * 访问consul token
     */
    private String aclToken;

    /**
     * 定时监听consul服务超时时间
     */
    private int catalogServicesWatchTimeout = 2;

    /**
     * 监听consul服务实例变化延时时间
     */
    private int catalogServicesWatchDelay = 1000;

    /**
     * 定时拉取服务实例间隔时间
     */
    private long refreshIntervalInMillis = 15000;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getAclToken() {
        return aclToken;
    }

    public void setAclToken(String aclToken) {
        this.aclToken = aclToken;
    }

    public int getCatalogServicesWatchTimeout() {
        return catalogServicesWatchTimeout;
    }

    public void setCatalogServicesWatchTimeout(int catalogServicesWatchTimeout) {
        this.catalogServicesWatchTimeout = catalogServicesWatchTimeout;
    }

    public int getCatalogServicesWatchDelay() {
        return catalogServicesWatchDelay;
    }

    public void setCatalogServicesWatchDelay(int catalogServicesWatchDelay) {
        this.catalogServicesWatchDelay = catalogServicesWatchDelay;
    }

    public long getRefreshIntervalInMillis() {
        return refreshIntervalInMillis;
    }

    public void setRefreshIntervalInMillis(long refreshIntervalInMillis) {
        this.refreshIntervalInMillis = refreshIntervalInMillis;
    }
}


