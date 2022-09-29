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

package com.huawei.discovery.consul.interceptors;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
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

import com.huawei.discovery.consul.entity.ServiceInstance;
import com.huawei.discovery.consul.retry.InvokerContext;
import com.huawei.discovery.consul.service.InvokerService;
import com.huawei.discovery.consul.service.LbService;
import com.huawei.discovery.consul.utils.HttpConstants;
import com.huawei.discovery.consul.utils.HttpParamersUtils;

import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.plugin.agent.entity.ExecuteContext;
import com.huaweicloud.sermant.core.plugin.agent.interceptor.Interceptor;
import com.huaweicloud.sermant.core.plugin.service.PluginServiceManager;
import com.huaweicloud.sermant.core.service.ServiceManager;

/**
 * 拦截获取服务列表
 *
 * @author chengyouling
 * @since 2022-9-14
 */
public class HttpClientInterceptor implements Interceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private final ThreadLocal<Boolean> mark = new ThreadLocal<>();

    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        /*final InvokerService invokerService = PluginServiceManager.getPluginService(InvokerService.class);
        HttpUriRequest httpUriRequest = (HttpUriRequest) context.getArguments()[0];
        String method = httpUriRequest.getMethod();
        URI uri = httpUriRequest.getURI();
        Map<String, String> hostAndPath = recoverHostAndPath(uri.getPath());
        String uriNew = buildNewUrl(uri,
                PluginServiceManager.getPluginService(LbService.class).choose(hostAndPath.get(HttpConstants.HTTP_URI_HOST)).get(),
                hostAndPath.get(HttpConstants.HTTP_URI_PATH), method);
        final Object originUri = context.getArguments()[0];
        context.getArguments()[0] = builNewRequest(uriNew, method, httpUriRequest);
        return context;*/
        if (mark.get() != null) {
            return context;
        }
        mark.set(Boolean.TRUE);
        try {
            final InvokerService invokerService = PluginServiceManager.getPluginService(InvokerService.class);
            HttpUriRequest httpUriRequest = (HttpUriRequest) context.getArguments()[0];
            String method = httpUriRequest.getMethod();
            URI uri = httpUriRequest.getURI();
            Map<String, String> hostAndPath = recoverHostAndPath(uri.getPath());
            final Function<InvokerContext, Object> function = invokerContext -> {
                String uriNew = buildNewUrl(uri, invokerContext.getServiceInstance(),
                        hostAndPath.get(HttpConstants.HTTP_URI_PATH), method);
                final Object originUri = context.getArguments()[0];
                context.getArguments()[0] = builNewRequest(uriNew, method, httpUriRequest);
                final Object result = buildFunc(context, invokerContext).get();
                context.getArguments()[0] = originUri;
                return result;
            };
            final Function<Exception, Object> exFunc = this::buildErrorResponse;
            final Optional<Object> invoke = invokerService
                    .invoke(function, exFunc, hostAndPath.get(HttpConstants.HTTP_URI_HOST));
            invoke.ifPresent(context::skip);
            return context;
        } finally {
            mark.remove();
        }
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

    private String buildNewUrl(URI uri, ServiceInstance serviceInstance, String path, String method) {
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

    private Map<String, String> recoverHostAndPath(String path) {
        Map<String, String> result = new HashMap<String, String>();
        if (StringUtils.isEmpty(path)) {
            return result;
        }
        int startIndex = 0;
        while (startIndex < path.length() && path.charAt(startIndex) == HttpConstants.HTTP_URL_SINGLE_SLASH) {
            startIndex++;
        }
        String tempPath = path.substring(startIndex);
        result.put(HttpConstants.HTTP_URI_HOST,
                tempPath.substring(0, tempPath.indexOf(HttpConstants.HTTP_URL_SINGLE_SLASH)));
        result.put(HttpConstants.HTTP_URI_PATH,
                tempPath.substring(tempPath.indexOf(HttpConstants.HTTP_URL_SINGLE_SLASH)));
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
