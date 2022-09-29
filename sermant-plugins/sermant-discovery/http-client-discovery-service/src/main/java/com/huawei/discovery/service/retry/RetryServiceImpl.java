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

package com.huawei.discovery.service.retry;

import com.huawei.discovery.consul.entity.Recorder;
import com.huawei.discovery.consul.entity.ServiceInstance;
import com.huawei.discovery.consul.retry.InvokerContext;
import com.huawei.discovery.consul.retry.Retry;
import com.huawei.discovery.consul.retry.Retry.RetryContext;
import com.huawei.discovery.consul.retry.RetryConfig;
import com.huawei.discovery.consul.retry.RetryException;
import com.huawei.discovery.consul.service.InvokerService;
import com.huawei.discovery.service.ex.ProviderException;
import com.huawei.discovery.service.lb.DiscoveryManager;
import com.huawei.discovery.service.lb.rule.AbstractLoadbalancer;
import com.huawei.discovery.service.lb.rule.RoundRobinLoadbalancer;
import com.huawei.discovery.service.lb.stats.InstanceStats;
import com.huawei.discovery.service.lb.stats.ServiceStatsManager;

import com.huaweicloud.sermant.core.common.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * 重试调用
 *
 * @author zhouss
 * @since 2022-09-28
 */
public class RetryServiceImpl implements InvokerService {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    /**
     * 最大配置数量
     */
    private static final int MAX_SIZE = 9999;

    private static final String NAME = "SERMANT_DEFAULT_RETRY";

    private final AbstractLoadbalancer retryLoadbalancer = new RoundRobinLoadbalancer();

    private final List<Class<? extends Throwable>> retryEx = Arrays.asList(
            ConnectException.class,
            SocketTimeoutException.class,
            NoRouteToHostException.class,
            IOException.class);

    private final Map<String, Retry> retryCache = new ConcurrentHashMap<>();

    private Retry defaultRetry;

    @Override
    public void start() {
        defaultRetry = Retry.create(new RetryConfig(retryEx, result -> false, NAME));
    }

    @Override
    public Optional<Object> invoke(Function<InvokerContext, Object> invokeFunc, Function<Exception, Object> exFunc,
            String serviceName) {
        return invoke(invokeFunc, exFunc, serviceName, getRetry(null));
    }

    @Override
    public Optional<Object> invoke(Function<InvokerContext, Object> invokeFunc, Function<Exception, Object> exFunc,
            String serviceName, RetryConfig retryConfig) {
        return invoke(invokeFunc, exFunc, serviceName, getRetry(retryConfig));
    }

    private Optional<Object> invoke(Function<InvokerContext, Object> invokeFunc, Function<Exception, Object> exFunc,
            String serviceName, Retry retry) {
        try {
            return invokeWithEx(invokeFunc, serviceName, retry);
        } catch (RetryException ex) {
            // 走异常处理流程
            return Optional.ofNullable(exFunc.apply(ex.getRealEx()));
        }catch (Exception ex) {
            // 重试最终失败抛出异常, 需对异常进行封装, 返回给上游, 或者作为当前调用返回调用方
            return Optional.ofNullable(exFunc.apply(ex));
        }
    }

    private Retry getRetry(RetryConfig retryConfig) {
        if (retryConfig == null) {
            return defaultRetry;
        }
        if (retryCache.size() >= MAX_SIZE) {
            LOGGER.warning(String.format(Locale.ENGLISH,
                    "Retry Config [%s] exceed max size [%s], it will replace it with default retry!",
                    retryConfig.getName(), MAX_SIZE));
            return defaultRetry;
        }
        return retryCache.computeIfAbsent(retryConfig.getName(), name -> Retry.create(retryConfig));
    }

    private Optional<Object> invokeWithEx(Function<InvokerContext, Object> invokeFunc, String serviceName, Retry retry)
            throws Exception {
        final RetryContext<Recorder> context = retry.context();
        final InvokerContext invokerContext = new InvokerContext();
        boolean isRetry = false;
        do {
            final long start = System.currentTimeMillis();
            final Optional<ServiceInstance> instance = choose(serviceName, isRetry);
            if (!instance.isPresent()) {
                throw new ProviderException("Can not found provider named: " + serviceName);
            }
            invokerContext.setServiceInstance(instance.get());
            final InstanceStats stats = ServiceStatsManager.INSTANCE.getInstanceStats(instance.get());
            context.onBefore(stats);
            long consumeTimeMs;
            try {
                final Object result = invokeFunc.apply(invokerContext);
                consumeTimeMs = System.currentTimeMillis() - start;
                if (invokerContext.getEx() != null) {
                    // 此处调用器, 若调用出现异常, 则以异常结果返回
                    context.onError(stats, (Exception) invokerContext.getEx(), consumeTimeMs);
                    invokerContext.setEx(null);
                    continue;
                }
                final boolean isNeedRetry = context.onResult(stats, result, consumeTimeMs);
                if (!isNeedRetry) {
                    context.onComplete(stats);
                    return Optional.ofNullable(result);
                }
            } catch (Exception ex) {
                context.onError(stats, ex, System.currentTimeMillis() - start);
            }
            isRetry = true;
        } while (true);
    }

    private Optional<ServiceInstance> choose(String serviceName, boolean isRetry) {
        if (isRetry) {
            // 处于重试中的实例采用轮询, 避免已经调用过的再次调用
            return DiscoveryManager.INSTANCE.choose(serviceName, retryLoadbalancer);
        }
        return DiscoveryManager.INSTANCE.choose(serviceName);
    }
}
