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
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.HttpParams;

import com.huawei.discovery.entity.ServiceInstance;
import com.huawei.discovery.retry.InvokerContext;
import com.huawei.discovery.service.InvokerService;
import com.huawei.discovery.utils.HttpConstants;

import com.huawei.discovery.utils.PlugEffectWhiteBlackUtils;
import com.huawei.discovery.utils.RequestInterceptorUtils;
import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.plugin.agent.entity.ExecuteContext;
import com.huaweicloud.sermant.core.plugin.agent.interceptor.Interceptor;
import com.huaweicloud.sermant.core.plugin.service.PluginServiceManager;
import com.huaweicloud.sermant.core.utils.StringUtils;

/**
 * 拦截获取服务列表
 *
 * @author chengyouling
 * @since 2022-9-14
 */
public class HttpClientInterceptor implements Interceptor {
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
            HttpUriRequest httpUriRequest = (HttpUriRequest) context.getArguments()[0];
            String method = httpUriRequest.getMethod();
            URI uri = httpUriRequest.getURI();
            if (!PlugEffectWhiteBlackUtils.isHostEqualRealmName(uri.getHost())) {
                return context;
            }
            Map<String, String> hostAndPath = RequestInterceptorUtils.recoverHostAndPath(uri.getPath());
            if (!PlugEffectWhiteBlackUtils.isPlugEffect(hostAndPath.get(HttpConstants.HTTP_URI_HOST))) {
                return context;
            }
            final Function<InvokerContext, Object> function = invokerContext -> {
                String uriNew = RequestInterceptorUtils.buildUrlWithIp(uri, invokerContext.getServiceInstance(),
                        hostAndPath.get(HttpConstants.HTTP_URI_PATH), method);
                context.getArguments()[0] = builNewRequest(uriNew, method, httpUriRequest);
                return RequestInterceptorUtils.buildFunc(context, invokerContext).get();
            };
            final Function<Exception, Object> exFunc = this::buildErrorResponse;
            final Optional<Object> invoke = invokerService
                    .invoke(function, exFunc, hostAndPath.get(HttpConstants.HTTP_URI_HOST));
            invoke.ifPresent(context::skip);
            if (PlugEffectWhiteBlackUtils.isOpenLogger()) {
                count.getAndIncrement();
                LOGGER.log(Level.SEVERE,
                        "currentTime: " + HttpConstants.currentTime() + "httpClientInterceptor effect count: " + count);
            }
            return context;
        } finally {
            mark.remove();
        }
    }

    private HttpUriRequest builNewRequest(String uriNew, String method, HttpUriRequest httpUriRequest) {
        if (HttpConstants.HTTP_GET.equals(method)) {
            return new HttpGet(uriNew);
        } else if (HttpConstants.HTTP_POST.equals(method)) {
            HttpPost oldHttpPost = (HttpPost) httpUriRequest;
            HttpPost httpPost = new HttpPost(uriNew);
            httpPost.setEntity(oldHttpPost.getEntity());
            return httpPost;
        }
        return httpUriRequest;
    }

    @Override
    public ExecuteContext after(ExecuteContext context) throws Exception {
        return context;
    }

    @Override
    public ExecuteContext onThrow(ExecuteContext context) throws Exception {
        return context;
    }

    private CloseableHttpResponse buildErrorResponse(Exception ex) {
        return new CloseableHttpResponse() {
            @Override
            public void close() throws IOException {

            }

            @Override
            public StatusLine getStatusLine() {
                return new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
            }

            @Override
            public void setStatusLine(StatusLine statusline) {

            }

            @Override
            public void setStatusLine(ProtocolVersion ver, int code) {

            }

            @Override
            public void setStatusLine(ProtocolVersion ver, int code, String reason) {

            }

            @Override
            public void setStatusCode(int code) throws IllegalStateException {

            }

            @Override
            public void setReasonPhrase(String reason) throws IllegalStateException {

            }

            @Override
            public HttpEntity getEntity() {
                return new StringEntity(ex.getMessage(), ContentType.APPLICATION_JSON);
            }

            @Override
            public void setEntity(HttpEntity entity) {

            }

            @Override
            public Locale getLocale() {
                return null;
            }

            @Override
            public void setLocale(Locale loc) {

            }

            @Override
            public ProtocolVersion getProtocolVersion() {
                return null;
            }

            @Override
            public boolean containsHeader(String name) {
                return false;
            }

            @Override
            public Header[] getHeaders(String name) {
                return new Header[0];
            }

            @Override
            public Header getFirstHeader(String name) {
                return null;
            }

            @Override
            public Header getLastHeader(String name) {
                return null;
            }

            @Override
            public Header[] getAllHeaders() {
                return new Header[0];
            }

            @Override
            public void addHeader(Header header) {

            }

            @Override
            public void addHeader(String name, String value) {

            }

            @Override
            public void setHeader(Header header) {

            }

            @Override
            public void setHeader(String name, String value) {

            }

            @Override
            public void setHeaders(Header[] headers) {

            }

            @Override
            public void removeHeader(Header header) {

            }

            @Override
            public void removeHeaders(String name) {

            }

            @Override
            public HeaderIterator headerIterator() {
                return null;
            }

            @Override
            public HeaderIterator headerIterator(String name) {
                return null;
            }

            @Override
            public HttpParams getParams() {
                return null;
            }

            @Override
            public void setParams(HttpParams params) {

            }
        };
    }
}
