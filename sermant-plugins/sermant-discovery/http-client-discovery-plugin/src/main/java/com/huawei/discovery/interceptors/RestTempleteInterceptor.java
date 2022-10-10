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
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import com.huawei.discovery.config.DiscoveryPluginConfig;
import com.huawei.discovery.retry.InvokerContext;
import com.huawei.discovery.service.InvokerService;
import com.huawei.discovery.utils.HttpConstants;
import com.huawei.discovery.utils.PlugEffectWhiteBlackUtils;
import com.huawei.discovery.utils.RequestInterceptorUtils;
import com.huaweicloud.sermant.core.plugin.agent.entity.ExecuteContext;
import com.huaweicloud.sermant.core.plugin.config.PluginConfigManager;
import com.huaweicloud.sermant.core.plugin.service.PluginServiceManager;

/**
 * 拦截获取服务列表
 *
 * @author chengyouling
 * @since 2022-9-27
 */
public class RestTempleteInterceptor extends MarkInterceptor {

    @Override
    protected ExecuteContext doBefore(ExecuteContext context) throws Exception {
        final InvokerService invokerService = PluginServiceManager.getPluginService(InvokerService.class);
        String url = (String)context.getArguments()[0];
        DiscoveryPluginConfig config = PluginConfigManager.getPluginConfig(DiscoveryPluginConfig.class);
        if (!PlugEffectWhiteBlackUtils.isUrlContainsRealmName(url, config.getRealmName())) {
            return context;
        }
        Map<String, String> urlInfo = RequestInterceptorUtils.recovertUrl(url);
        if (!PlugEffectWhiteBlackUtils.isPlugEffect(urlInfo.get(HttpConstants.HTTP_URI_HOST))) {
            return context;
        }
        final Function<InvokerContext, Object> function = invokerContext -> {
            context.getArguments()[0] = RequestInterceptorUtils.buildUrl(urlInfo, invokerContext.getServiceInstance());
            return RequestInterceptorUtils.buildFunc(context, invokerContext).get();
        };
        final Function<Exception, Object> exFunc = this::buildErrorResponse;
        final Optional<Object> invoke = invokerService
                .invoke(function, exFunc, urlInfo.get(HttpConstants.HTTP_URI_HOST));
        invoke.ifPresent(context::skip);
        return context;
    }

    /**
     * 构建restTemplete响应
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

    @Override
    public ExecuteContext after(ExecuteContext context) throws Exception {
        return context;
    }

    @Override
    public ExecuteContext onThrow(ExecuteContext context) throws Exception {
        return context;
    }
}
