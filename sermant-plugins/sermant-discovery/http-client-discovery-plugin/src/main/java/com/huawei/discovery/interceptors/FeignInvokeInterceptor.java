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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpStatus;

import com.huawei.discovery.retry.InvokerContext;
import com.huawei.discovery.service.InvokerService;
import com.huawei.discovery.utils.HttpConstants;
import com.huawei.discovery.utils.PlugEffectWhiteBlackUtils;
import com.huawei.discovery.utils.RequestInterceptorUtils;
import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.plugin.agent.entity.ExecuteContext;
import com.huaweicloud.sermant.core.plugin.agent.interceptor.Interceptor;
import com.huaweicloud.sermant.core.plugin.service.PluginServiceManager;

import feign.Request;
import feign.Response;

/**
 * 拦截获取服务列表
 *
 * @author chengyouling
 * @since 2022-9-27
 */
public class FeignInvokeInterceptor implements Interceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    private static AtomicInteger count = new AtomicInteger(0);

    private final ThreadLocal<Boolean> mark = new ThreadLocal<>();

    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        if (mark.get() != null) {
            return context;
        }
        mark.set(Boolean.TRUE);
        try {
            final InvokerService invokerService = PluginServiceManager.getPluginService(InvokerService.class);
            Request request = (Request)context.getArguments()[0];
            if (!PlugEffectWhiteBlackUtils.isUrlContainsRealmName(request.url())) {
                return context;
            }
            Map<String, String> urlInfo = RequestInterceptorUtils.recovertUrl(request.url());
            if (!PlugEffectWhiteBlackUtils.isPlugEffect(urlInfo.get(HttpConstants.HTTP_URI_HOST))) {
                return context;
            }
            final Function<InvokerContext, Object> function = invokerContext -> {
                context.getArguments()[0] = Request.create(request.httpMethod(),
                        RequestInterceptorUtils.buildUrl(urlInfo, invokerContext.getServiceInstance()), request.headers(), request.requestBody());
                return RequestInterceptorUtils.buildFunc(context, invokerContext).get();
            };
            final Function<Exception, Object> exFunc = this::buildErrorResponse;
            final Optional<Object> invoke = invokerService
                    .invoke(function, exFunc, urlInfo.get(HttpConstants.HTTP_URI_HOST));
            invoke.ifPresent(context::skip);
            if (PlugEffectWhiteBlackUtils.isOpenLogger()) {
                count.getAndIncrement();
                LOGGER.log(Level.INFO,
                        "currentTime: " + HttpConstants.currentTime() + "feignInvokeInterceptor effect count: " + count);
            }
            return context;
        } finally {
            mark.remove();
        }
    }

    /**
     * 构建feign响应
     * @param ex
     * @return
     */
    private Response buildErrorResponse(Exception ex) {
        Response.Builder builder = Response.builder();
        builder.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        builder.reason(ex.getMessage());
        return builder.build();
    }

    @Override
    public ExecuteContext after(ExecuteContext context) throws Exception {
        return context;
    }

    @Override
    public ExecuteContext onThrow(ExecuteContext context) throws Exception {
        return context;
    }
}
