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

package com.huawei.discovery.service.lb.rule;

import com.huawei.discovery.consul.entity.ServiceInstance;
import com.huawei.discovery.service.lb.stats.ServiceStats;
import com.huawei.discovery.service.lb.stats.ServiceStatsManager;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 基于响应时间的负载均衡
 *
 * @author zhouss
 * @since 2022-09-29
 */
public class WeightedResponseTimeLoadbalancer extends AbstractLoadbalancer {
    private final AbstractLoadbalancer defaultLb = new RoundRobinLoadbalancer();

    @Override
    protected ServiceInstance doChoose(String serviceName, List<ServiceInstance> instances) {
        final ServiceStats serviceStats = ServiceStatsManager.INSTANCE.getServiceStats(serviceName);
        List<Double> responseTimeWeights = serviceStats.getResponseTimeWeights();
        if (responseTimeWeights == null) {
            return defaultLb.doChoose(serviceName, instances);
        }
        if (responseTimeWeights.size() != instances.size()) {
            // 采取强制刷新
            serviceStats.aggregationStats(instances);
            responseTimeWeights = serviceStats.getResponseTimeWeights();
        }
        double maxWeights = responseTimeWeights.get(responseTimeWeights.size() - 1);
        final double seed = ThreadLocalRandom.current().nextDouble(maxWeights);
        ServiceInstance result = instances.get(0);
        for (int i = 0, size = responseTimeWeights.size(); i < size; i++) {
            if (seed <= responseTimeWeights.get(i)) {
                result = instances.get(i);
                break;
            }
        }
        return result;
    }

    @Override
    public String lbType() {
        return "WeightedResponseTime";
    }
}
