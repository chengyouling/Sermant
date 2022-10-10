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

package com.huawei.discovery.interceptors;

import com.huawei.discovery.config.DiscoveryPluginConfig;
import com.huawei.discovery.config.LbConfig;
import com.huawei.discovery.utils.HttpConstants;
import com.huawei.discovery.utils.PlugEffectWhiteBlackUtils;

import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.plugin.agent.entity.ExecuteContext;
import com.huaweicloud.sermant.core.plugin.agent.interceptor.Interceptor;
import com.huaweicloud.sermant.core.plugin.config.PluginConfigManager;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 用于标记拦截器调用, 仅用于调用{@link com.huawei.discovery.service.InvokerService}得拦截器使用
 *
 * @author zhouss
 * @since 2022-10-10
 */
public abstract class MarkInterceptor implements Interceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private final AtomicInteger requestCount = new AtomicInteger();

    private final ThreadLocal<Boolean> mark = new ThreadLocal<Boolean>();

    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        if (mark.get() != null) {
            return context;
        }
        mark.set(Boolean.TRUE);
        try {
            return doBefore(context);
        } finally {
            DiscoveryPluginConfig config = PluginConfigManager.getPluginConfig(DiscoveryPluginConfig.class);
            if (config.isLoggerFlag()) {
                requestCount.getAndIncrement();
                LOGGER.log(Level.INFO,
                        "currentTime: " + HttpConstants.currentTime() + "httpClientInterceptor effect count: " + requestCount);
            }
            mark.remove();
        }
    }

    /**
     * 调用逻辑
     *
     * @param context 上下文
     * @return 上下文
     * @throws Exception 执行异常抛出
     */
    protected abstract ExecuteContext doBefore(ExecuteContext context) throws Exception;
}
