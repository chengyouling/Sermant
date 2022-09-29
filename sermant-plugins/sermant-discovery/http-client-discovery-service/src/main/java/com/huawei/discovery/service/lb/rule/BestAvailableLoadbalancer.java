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
import com.huawei.discovery.service.lb.stats.InstanceStats;
import com.huawei.discovery.service.lb.stats.ServiceStatsManager;

import java.util.List;

/**
 * 最低并发负载均衡
 *
 * @author zhouss
 * @since 2022-09-29
 */
public class BestAvailableLoadbalancer extends AbstractLoadbalancer {
    @Override
    protected ServiceInstance doChoose(String serviceName, List<ServiceInstance> instances) {
        long minActiveRequest = Long.MAX_VALUE;
        ServiceInstance result = instances.get(0);
        for (ServiceInstance serviceInstance : instances) {
            final InstanceStats instanceStats = ServiceStatsManager.INSTANCE.getInstanceStats(serviceInstance);
            final long activeRequest = instanceStats.getActiveRequests().get();
            if (minActiveRequest > activeRequest) {
                result = serviceInstance;
                minActiveRequest = activeRequest;
            }
        }
        return result;
    }

    @Override
    public String lbType() {
        return "BestAvailable";
    }
}
