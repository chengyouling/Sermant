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

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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

import okhttp3.HttpUrl;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Response.Builder;

/**
 * 拦截获取服务列表
 *
 * @author chengyouling
 * @since 2022-09-14
 */
public class OkHttp3ClientInterceptor implements Interceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    private final ThreadLocal<Boolean> mark = new ThreadLocal<>();

    private static AtomicInteger count = new AtomicInteger(0);

    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        if (mark.get() != null) {
            return context;
        }
        mark.set(Boolean.TRUE);
        try {
            final InvokerService invokerService = PluginServiceManager.getPluginService(InvokerService.class);
            Request request = (Request)context.getRawMemberFieldValue("originalRequest");
            URI uri = request.url().uri();
            if (!PlugEffectWhiteBlackUtils.isHostEqualRealmName(uri.getHost())) {
                return context;
            }
            String method = request.method();
            Map<String, String> hostAndPath = RequestInterceptorUtils.recoverHostAndPath(uri.getPath());
            if (!PlugEffectWhiteBlackUtils.isPlugEffect(hostAndPath.get(HttpConstants.HTTP_URI_HOST))) {
                return context;
            }
            AtomicReference<Request> rebuildRequest = new AtomicReference<>();
            final Function<InvokerContext, Object> function = invokerContext -> {
                String url = RequestInterceptorUtils.buildUrlWithIp(uri, invokerContext.getServiceInstance(), hostAndPath.get(HttpConstants.HTTP_URI_PATH), method);
                HttpUrl newUrl = HttpUrl.parse(url);
                Request newRequest = request
                        .newBuilder()
                        .url(newUrl)
                        .build();
                rebuildRequest.set(newRequest);
                try {
                    context.setRawMemberFieldValue("originalRequest", newRequest);
                } catch (Exception e) {
                    LOGGER.warning("setRawMemberFieldValue originalRequest failed");
                    return context;
                }
                return RequestInterceptorUtils.buildFunc(context, invokerContext).get();
            };
            final Function<Exception, Object> exFunc = new Function<Exception, Object>() {
                @Override
                public Object apply(Exception e) {
                    return buildErrorResponse(e, rebuildRequest.get());
                }
            };
            final Optional<Object> invoke = invokerService
                    .invoke(function, exFunc, hostAndPath.get(HttpConstants.HTTP_URI_HOST));
            invoke.ifPresent(context::skip);
            if (PlugEffectWhiteBlackUtils.isOpenLogger()) {
                count.getAndIncrement();
                LOGGER.log(Level.SEVERE,
                        "currentTime: " + HttpConstants.currentTime() + "okHttp3ClientInterceptor effect count: " + count);
            }
            return context;
        } finally {
            mark.remove();
        }
    }

    /**
     * 构建okHttp3响应
     * @param ex
     * @return
     */
    private Response buildErrorResponse(Exception ex, Request request) {
        Builder builder = new Builder();
        builder.code(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        builder.message(ex.getMessage());
        builder.protocol(Protocol.HTTP_1_1);
        builder.request(request);
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
