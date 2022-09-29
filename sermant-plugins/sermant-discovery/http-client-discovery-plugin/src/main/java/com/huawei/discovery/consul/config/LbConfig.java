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

package com.huawei.discovery.consul.config;

import com.huaweicloud.sermant.core.config.common.ConfigTypeKey;
import com.huaweicloud.sermant.core.plugin.config.PluginConfig;

/**
 * 负载均衡配置
 *
 * @author zhouss
 * @since 2022-09-26
 */
@ConfigTypeKey("sermant.discovery.lb")
public class LbConfig implements PluginConfig {
    /**
     * 注册中心地址
     */
    private String registryAddress = "127.0.0.1:2181";

    /**
     * 连接超时时间
     */
    private int connectionTimeoutMs = 2000;

    /**
     * 响应超时时间
     */
    private int readTimeoutMs = 10000;

    /**
     * 连接重试时间
     */
    private int retryIntervalMs = 30000;

    /**
     * zookeeper保存数据的前缀
     */
    private String zkBasePath = "/services";

    /**
     * 缓存获取时间
     */
    private long cacheExpireMs = 30000L;

    /**
     * 缓存自动刷新时间
     */
    private long refreshIntervalMs = 60000L;

    /**
     * 缓存并发度, 影响从缓存获取实例的效率
     */
    private int cacheConcurrencyLevel = 16;

    /**
     * 负载均衡类型
     */
    private String lbType = "RoundRobin";

    /**
     * 倾向IP, 若为true, 则所有关联的地址均有ip替换host
     */
    private boolean preferIpAddress = false;

    /**
     * 服务指标数据缓存, 默认60分钟
     */
    private long statsCacheExpireTime = 60;

    /**
     * 统计数据定时聚合统计刷新时间, 若设置<=0, 则不会开启聚合统计, 关联聚合统计的负载均衡将会失效
     */
    private long lbStatsRefreshIntervalMs = 30000L;

    public long getLbStatsRefreshIntervalMs() {
        return lbStatsRefreshIntervalMs;
    }

    public void setLbStatsRefreshIntervalMs(long lbStatsRefreshIntervalMs) {
        this.lbStatsRefreshIntervalMs = lbStatsRefreshIntervalMs;
    }

    public long getStatsCacheExpireTime() {
        return statsCacheExpireTime;
    }

    public void setStatsCacheExpireTime(long statsCacheExpireTime) {
        this.statsCacheExpireTime = statsCacheExpireTime;
    }

    public long getRefreshIntervalMs() {
        return refreshIntervalMs;
    }

    public void setRefreshIntervalMs(long refreshIntervalMs) {
        this.refreshIntervalMs = refreshIntervalMs;
    }

    public int getCacheConcurrencyLevel() {
        return cacheConcurrencyLevel;
    }

    public void setCacheConcurrencyLevel(int cacheConcurrencyLevel) {
        this.cacheConcurrencyLevel = cacheConcurrencyLevel;
    }

    public boolean isPreferIpAddress() {
        return preferIpAddress;
    }

    public void setPreferIpAddress(boolean preferIpAddress) {
        this.preferIpAddress = preferIpAddress;
    }

    public String getLbType() {
        return lbType;
    }

    public void setLbType(String lbType) {
        this.lbType = lbType;
    }

    public long getCacheExpireMs() {
        return cacheExpireMs;
    }

    public void setCacheExpireMs(long cacheExpireMs) {
        this.cacheExpireMs = cacheExpireMs;
    }

    public String getZkBasePath() {
        return zkBasePath;
    }

    public void setZkBasePath(String zkBasePath) {
        this.zkBasePath = zkBasePath;
    }

    public int getRetryIntervalMs() {
        return retryIntervalMs;
    }

    public void setRetryIntervalMs(int retryIntervalMs) {
        this.retryIntervalMs = retryIntervalMs;
    }

    public String getRegistryAddress() {
        return registryAddress;
    }

    public void setRegistryAddress(String registryAddress) {
        this.registryAddress = registryAddress;
    }

    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(int connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
}
