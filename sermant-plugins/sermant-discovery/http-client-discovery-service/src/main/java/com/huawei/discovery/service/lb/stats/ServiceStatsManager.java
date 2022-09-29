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

package com.huawei.discovery.service.lb.stats;

import com.huawei.discovery.consul.config.LbConfig;
import com.huawei.discovery.consul.entity.ServiceInstance;
import com.huawei.discovery.consul.factory.RealmServiceThreadFactory;

import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.plugin.config.PluginConfigManager;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * 服务统计数据管理器, 主要记录对应服务的指标数据
 *
 * @author zhouss
 * @since 2022-09-29
 */
public enum ServiceStatsManager {
    /**
     * 单例
     */
    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger();

    private static final String THREAD_NAME = "SERMANT_STATS_THREAD";

    private final Map<String, ServiceStats> serverStatsCache = new ConcurrentHashMap<>();

    final ScheduledExecutorService statsService = new ScheduledThreadPoolExecutor(1,
            new RealmServiceThreadFactory(THREAD_NAME));

    /**
     * 启动方法
     */
    public void start() {
        final long lbStatsRefreshIntervalMs = PluginConfigManager.getPluginConfig(LbConfig.class)
                .getLbStatsRefreshIntervalMs();
        if (lbStatsRefreshIntervalMs <= 0) {
            return;
        }
        LOGGER.info(String.format(Locale.ENGLISH, "Started stats time task with interval [%s]ms",
                lbStatsRefreshIntervalMs));
        statsService.scheduleAtFixedRate(this::aggregationStats, 0, lbStatsRefreshIntervalMs, TimeUnit.MILLISECONDS);
    }

    private void aggregationStats() {
        for (ServiceStats serviceStats : serverStatsCache.values()) {
            serviceStats.aggregationStats();
        }
    }

    /**
     * 结束方法
     */
    public void stop() {
        statsService.shutdown();
    }

    /**
     * 获取服务统计数据
     *
     * @param serviceName 服务名
     * @return ServiceStats
     */
    public ServiceStats getServiceStats(String serviceName) {
        return serverStatsCache.computeIfAbsent(serviceName, ServiceStats::new);
    }

    /**
     * 获取实例的指标统计数据
     *
     * @param serviceInstance 实例
     * @return 获取该实例的状态数据
     */
    public InstanceStats getInstanceStats(ServiceInstance serviceInstance) {
        final ServiceStats serviceStats = getServiceStats(serviceInstance.serviceName());
        return serviceStats.getStats(serviceInstance);
    }
}
