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

package com.huawei.discovery.consul.retry;

import java.util.List;
import java.util.function.Predicate;

/**
 * 重试配置
 *
 * @author zhouss
 * @since 2022-09-28
 */
public class RetryConfig {
    private static final int DEFAULT_MAX_RETRY = 3;

    private static final long DEFAULT_WAIT_RETRY_MS = 1000L;

    /**
     * 最大重试次数
     */
    private final int maxRetry;

    /**
     * 配置名称
     */
    private final String name;

    /**
     * 异常判断
     */
    private final Predicate<Throwable> throwablePredicate;

    /**
     * 结果判断
     */
    private final Predicate<Object> resultPredicate;

    private final long retryRetryWaitMs;

    /**
     * 构造器
     *
     * @param retryEx 重试异常集合
     * @param resultPredicate  结果判断
     * @param name 配置名
     */
    public RetryConfig(List<Class<? extends Throwable>> retryEx,
            Predicate<Object> resultPredicate, String name) {
        this.throwablePredicate = ex -> {
            if (ex == null) {
                return false;
            }
            for (Class<? extends Throwable> cur : retryEx) {
                if (cur.isAssignableFrom(ex.getClass())) {
                    return true;
                }
            }
            return false;
        };
        this.resultPredicate = resultPredicate;
        this.maxRetry = DEFAULT_MAX_RETRY;
        this.retryRetryWaitMs = DEFAULT_WAIT_RETRY_MS;
        this.name = name;
    }

    /**
     * 构造器
     *
     * @param throwablePredicate 异常判断
     * @param resultPredicate  结果判断
     * @param name 配置名
     */
    public RetryConfig(Predicate<Throwable> throwablePredicate,
            Predicate<Object> resultPredicate, String name) {
        this(throwablePredicate, resultPredicate, DEFAULT_MAX_RETRY, name);
    }

    /**
     * 构造器
     *
     * @param throwablePredicate 异常判断
     * @param resultPredicate  结果判断
     * @param maxRetry 最大重试次数
     * @param name 配置名
     */
    public RetryConfig(Predicate<Throwable> throwablePredicate,
            Predicate<Object> resultPredicate, int maxRetry, String name) {
        this.maxRetry = maxRetry;
        this.throwablePredicate = throwablePredicate;
        this.resultPredicate = resultPredicate;
        this.retryRetryWaitMs = DEFAULT_WAIT_RETRY_MS;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public long getRetryRetryWaitMs() {
        return retryRetryWaitMs;
    }

    public int getMaxRetry() {
        return maxRetry;
    }

    public Predicate<Throwable> getThrowablePredicate() {
        return throwablePredicate;
    }

    public Predicate<Object> getResultPredicate() {
        return resultPredicate;
    }
}
