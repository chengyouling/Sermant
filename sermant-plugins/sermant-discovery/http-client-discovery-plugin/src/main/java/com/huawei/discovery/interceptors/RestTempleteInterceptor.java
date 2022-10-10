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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

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

/**
 * 拦截获取服务列表
 *
 * @author chengyouling
 * @since 2022-9-27
 */
public class RestTempleteInterceptor implements Interceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    private final ThreadLocal<Boolean> mark = new ThreadLocal<Boolean>();

    private static AtomicInteger count = new AtomicInteger(0);

    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        if (mark.get() != null) {
            return context;
        }
        mark.set(Boolean.TRUE);
        try {
            final InvokerService invokerService = PluginServiceManager.getPluginService(InvokerService.class);
            String url = (String)context.getArguments()[0];
            final RealmNameConfig realmNameConfig = PluginConfigManager.getPluginConfig(RealmNameConfig.class);
            if (!url.contains(realmNameConfig.getCurrentRealmName())) {
                return context;
            }
            Map<String, String> urlParamers = recovertUrl(url);
            if (!PlugEffectWhiteBlackConstants.isPlugEffect(urlParamers.get(HttpConstants.HTTP_URI_HOST))) {
                return context;
            }
            final Function<InvokerContext, Object> function = invokerContext -> {
                context.getArguments()[0] = buildUrl(urlParamers, invokerContext.getServiceInstance());
                final Object result = buildFunc(context, invokerContext).get();
                return result;
            };
            final Function<Exception, Object> exFunc = this::buildErrorResponse;
            final Optional<Object> invoke = invokerService
                    .invoke(function, exFunc, urlParamers.get(HttpConstants.HTTP_URI_HOST));
            invoke.ifPresent(context::skip);
            if (PlugEffectWhiteBlackConstants.isOpenLogger()) {
                count.getAndIncrement();
                LOGGER.log(Level.SEVERE,
                        "currentTime: " + HttpConstants.currentTime() + "restTempleteInterceptor effect count: " + count);
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
    private ClientHttpResponse buildErrorResponse(Exception ex) {
        return new ClientHttpResponse() {

            @Override
            public HttpHeaders getHeaders() {
                return null;
            }

            @Override
            public InputStream getBody() throws IOException {
                return null;
            }

            @Override
            public HttpStatus getStatusCode() throws IOException {
                return HttpStatus.INTERNAL_SERVER_ERROR;
            }

            @Override
            public int getRawStatusCode() throws IOException {
                return HttpStatus.INTERNAL_SERVER_ERROR.value();
            }

            @Override
            public String getStatusText() throws IOException {
                return null;
            }

            @Override
            public void close() {

            }
        };
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

    /**
     * 解析url参数信息
     * http://gateway.com.cn/serviceName/sayHell?name=1
     * @param url
     * @return
     */
    public Map<String, String> recovertUrl(String url) {
        if (StringUtils.isEmpty(url)) {
            return null;
        }
        Map<String, String> result = new HashMap<String, String>();
        String scheme = url.substring(0, url.indexOf(HttpConstants.HTTP_URL_DOUBLIE_SLASH));
        String temp =url.substring(url.indexOf(HttpConstants.HTTP_URL_DOUBLIE_SLASH) + 3);
        //剔除域名之后的path
        temp = temp.substring(temp.indexOf(HttpConstants.HTTP_URL_SINGLE_SLASH) + 1);
        //服务名
        String host = temp.substring(0, temp.indexOf(HttpConstants.HTTP_URL_SINGLE_SLASH));
        //请求路径
        String path = temp.substring(temp.indexOf(HttpConstants.HTTP_URL_SINGLE_SLASH));
        result.put(HttpConstants.HTTP_URI_HOST, host);
        result.put(HttpConstants.HTTP_URL_SCHEME, scheme);
        result.put(HttpConstants.HTTP_URI_PATH, path);
        return result;
    }

    /**
     * 构建ip+端口url
     * @param urlParamers
     * @param serviceInstance
     * @return
     */
    private String buildUrl(Map<String, String> urlParamers, ServiceInstance serviceInstance) {
        StringBuilder urlBuild = new StringBuilder();
        urlBuild.append(urlParamers.get(HttpConstants.HTTP_URL_SCHEME))
                .append(HttpConstants.HTTP_URL_DOUBLIE_SLASH)
                .append(serviceInstance.getIp())
                .append(HttpConstants.HTTP_URL_COLON)
                .append(serviceInstance.getPort())
                .append(HttpConstants.HTTP_URI_PATH);
        return urlBuild.toString();
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
