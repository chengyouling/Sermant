/*
 * Copyright (C) 2021-2021 Huawei Technologies Co., Ltd. All rights reserved.
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

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpStatus;

import com.huawei.discovery.config.PlugEffectWhiteBlackConstants;
import com.huawei.discovery.config.RealmNameConfig;
import com.huawei.discovery.entity.ServiceInstance;
import com.huawei.discovery.retry.InvokerContext;
import com.huawei.discovery.service.InvokerService;
import com.huawei.discovery.utils.HttpConstants;
import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.plugin.agent.entity.ExecuteContext;
import com.huaweicloud.sermant.core.plugin.agent.interceptor.Interceptor;
import com.huaweicloud.sermant.core.plugin.config.PluginConfigManager;
import com.huaweicloud.sermant.core.plugin.service.PluginServiceManager;
import com.huaweicloud.sermant.core.utils.StringUtils;

import okhttp3.HttpUrl;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Response.Builder;

/**
 * 拦截获取服务列表
 *
 * @author chengyouling
 * @since 2022-9-14
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
            final RealmNameConfig realmNameConfig = PluginConfigManager.getPluginConfig(RealmNameConfig.class);
            if (!StringUtils.equalsIgnoreCase(uri.getHost(), realmNameConfig.getCurrentRealmName())) {
                return context;
            }
            String method = request.method();
            Map<String, String> hostAndPath = recoverHostAndPath(uri.getPath());
            if (!PlugEffectWhiteBlackConstants.isPlugEffect(hostAndPath.get(HttpConstants.HTTP_URI_HOST))) {
                return context;
            }
            AtomicReference<Request> rebuildRequest = new AtomicReference<>();
            final Function<InvokerContext, Object> function = invokerContext -> {
                String url = buildNewUrl(uri, invokerContext.getServiceInstance(), hostAndPath.get(HttpConstants.HTTP_URI_PATH), method);
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
                final Object result = buildFunc(context, invokerContext).get();
                return result;
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
            if (PlugEffectWhiteBlackConstants.isOpenLogger()) {
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
     * 构建feign响应
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

    private Supplier<Object> buildFunc(ExecuteContext context, InvokerContext invokerContext) {
        return () -> {
            try {
                return context.getMethod().invoke(context.getObject(), context.getArguments());
            } catch (IllegalAccessException e) {
                LOGGER.log(Level.SEVERE, String.format(Locale.ENGLISH, "Can not invoke method [%s]",
                        context.getMethod().getName()), e);
            } catch (InvocationTargetException e) {
                invokerContext.setEx(e.getTargetException());
                LOGGER.log(Level.FINE, String.format(Locale.ENGLISH, "invoke method [%s] failed",
                        context.getMethod().getName()), e);
            }
            return null;
        };
    }

    public static String buildNewUrl(URI uri, ServiceInstance serviceInstance, String path, String method) {
        StringBuilder urlBuild = new StringBuilder();
        urlBuild.append(uri.getScheme())
                .append(HttpConstants.HTTP_URL_DOUBLIE_SLASH)
                .append(serviceInstance.getIp())
                .append(HttpConstants.HTTP_URL_COLON)
                .append(serviceInstance.getPort())
                .append(path);
        if (method.equals(HttpConstants.HTTP_GET)) {
            urlBuild.append(HttpConstants.HTTP_URL_UNKNOWN)
                    .append(uri.getQuery());
        }
        return urlBuild.toString();
    }

    public static Map<String, String> recoverHostAndPath(String path) {
        Map<String, String> result = new HashMap<String, String>();
        if (StringUtils.isEmpty(path)) {
            return result;
        }
        int startIndex = 0;
        while (startIndex < path.length() && path.charAt(startIndex) == HttpConstants.HTTP_URL_SINGLE_SLASH) {
            startIndex++;
        }
        String tempPath = path.substring(startIndex);
        result.put(HttpConstants.HTTP_URI_HOST, tempPath.substring(0, tempPath.indexOf(HttpConstants.HTTP_URL_SINGLE_SLASH)));
        result.put(HttpConstants.HTTP_URI_PATH, tempPath.substring(tempPath.indexOf(HttpConstants.HTTP_URL_SINGLE_SLASH)));
        return result;
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
