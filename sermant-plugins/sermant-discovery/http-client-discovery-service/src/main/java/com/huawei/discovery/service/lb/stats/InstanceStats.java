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

import com.huawei.discovery.consul.entity.Recorder;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 记录当前实例的指标数据
 *
 * @author zhouss
 * @since 2022-09-26
 */
public class InstanceStats implements Recorder {
    /**
     * 正在请求的数量
     */
    private final AtomicLong activeRequests = new AtomicLong();

    /**
     * 所有的请求数统计
     */
    private final AtomicLong allRequestCount = new AtomicLong();

    /**
     * 所有请求调用的消耗时间
     */
    private final AtomicLong allRequestConsumeTime = new AtomicLong();

    /**
     * 请求失败数
     */
    private final AtomicLong failRequestCount = new AtomicLong();

    /**
     * 平均响应时间
     */
    private double responseAvgTime = 0d;

    /**
     * 调用前请求
     */
    @Override
    public void beforeRequest() {
        activeRequests.incrementAndGet();
        allRequestCount.incrementAndGet();
    }

    /**
     * 异常调用统计
     *
     * @param ex 异常类型
     */
    @Override
    public void errorRequest(Throwable ex, long consumeTimeMs) {
        baseStats(consumeTimeMs);
        failRequestCount.incrementAndGet();
    }

    /**
     * 结果调用
     */
    @Override
    public void afterRequest(long consumeTimeMs) {
        baseStats(consumeTimeMs);
    }

    private void baseStats(long consumeTimeMs) {
        final long request = activeRequests.decrementAndGet();
        if (request < 0) {
            activeRequests.set(0);
        }
        final long responseTime = allRequestConsumeTime.addAndGet(consumeTimeMs);
        responseAvgTime = responseTime * 1d / allRequestCount.get();
    }

    /**
     * 结束请求
     */
    @Override
    public void completeRequest() {
        allRequestCount.incrementAndGet();
    }

    public AtomicLong getAllRequestCount() {
        return allRequestCount;
    }

    public AtomicLong getAllRequestConsumeTime() {
        return allRequestConsumeTime;
    }

    public AtomicLong getActiveRequests() {
        return activeRequests;
    }

    public AtomicLong getFailRequestCount() {
        return failRequestCount;
    }

    public double getResponseAvgTime() {
        return responseAvgTime;
    }
}
