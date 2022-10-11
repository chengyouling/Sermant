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

package com.huawei.discovery.interceptors.httpclient;

import com.huawei.discovery.entity.ErrorCloseableHttpResponse;
import com.huawei.discovery.interceptors.MarkInterceptor;
import com.huawei.discovery.retry.InvokerContext;
import com.huawei.discovery.service.InvokerService;
import com.huawei.discovery.utils.HttpConstants;
import com.huawei.discovery.utils.PlugEffectWhiteBlackUtils;
import com.huawei.discovery.utils.RequestInterceptorUtils;

import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.plugin.agent.entity.ExecuteContext;
import com.huaweicloud.sermant.core.plugin.service.PluginServiceManager;
import com.huaweicloud.sermant.core.utils.ClassUtils;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * 仅针对4.x版本得http拦截
 *
 * @author zhouss
 * @since 2022-10-10
 */
public class HttpClient4xInterceptor extends MarkInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private final AtomicBoolean isLoaded = new AtomicBoolean();

    @Override
    public ExecuteContext doBefore(ExecuteContext context) {
        checkLoad();
        final InvokerService invokerService = PluginServiceManager.getPluginService(InvokerService.class);
        HttpHost httpHost = (HttpHost) context.getArguments()[0];
        final HttpRequest httpRequest = (HttpRequest) context.getArguments()[1];
        final Optional<URI> optionalUri = formatUri(httpRequest.getRequestLine().getUri());
        if (!optionalUri.isPresent()) {
            return context;
        }
        URI uri = optionalUri.get();
        Map<String, String> hostAndPath = RequestInterceptorUtils.recoverHostAndPath(uri.getPath());
        if (PlugEffectWhiteBlackUtils.isNotAllowRun(httpHost.getHostName(), hostAndPath.get(HttpConstants.HTTP_URI_HOST), true)) {
            return context;
        }
        invokerService.invoke(
                buildInvokerFunc(hostAndPath, uri, httpRequest, context),
                buildExFunc(httpRequest),
                hostAndPath.get(HttpConstants.HTTP_URI_HOST))
                .ifPresent(context::skip);
        return context;
    }

    private Function<InvokerContext, Object> buildInvokerFunc(Map<String, String> hostAndPath, URI uri,
            HttpRequest httpRequest, ExecuteContext context) {
        final String method = httpRequest.getRequestLine().getMethod();
        return invokerContext -> {
            String uriNew = RequestInterceptorUtils.buildUrlWithIp(uri, invokerContext.getServiceInstance(),
                    hostAndPath.get(HttpConstants.HTTP_URI_PATH), method);
            context.getArguments()[0] = rebuildHttpHost(uriNew);
            context.getArguments()[1] = rebuildRequest(uriNew, method, httpRequest);
            return RequestInterceptorUtils.buildFunc(context, invokerContext).get();
        };
    }

    private Function<Exception, Object> buildExFunc(HttpRequest httpRequest) {
        return ex -> new ErrorCloseableHttpResponse(ex, httpRequest.getProtocolVersion());
    }

    private void checkLoad() {
        if (isLoaded.compareAndSet(false, true)) {
            ClassUtils.defineClass("com.huawei.discovery.entity.ErrorCloseableHttpResponse",
                    Thread.currentThread().getContextClassLoader());
        }
    }

    private Optional<URI> formatUri(String uri) {
        if (!isValidUrl(uri)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new URI(uri));
        } catch (URISyntaxException e) {
            LOGGER.fine(String.format(Locale.ENGLISH, "%s is not valid uri!", uri));
            return Optional.empty();
        }
    }

    private boolean isValidUrl(String url) {
        final String lowerCaseUrl = url.toLowerCase(Locale.ROOT);
        return lowerCaseUrl.startsWith("http") || lowerCaseUrl.startsWith("https");
    }

    private HttpHost rebuildHttpHost(String uriNew) {
        final Optional<URI> optionalUri = formatUri(uriNew);
        if (optionalUri.isPresent()) {
            return URIUtils.extractHost(optionalUri.get());
        }
        throw new IllegalArgumentException("Invalid url: " + uriNew);
    }

    private HttpRequest rebuildRequest(String uriNew, String method, HttpRequest httpUriRequest) {
        if (httpUriRequest instanceof HttpPost) {
            HttpPost oldHttpPost = (HttpPost) httpUriRequest;
            HttpPost httpPost = new HttpPost(uriNew);
            httpPost.setEntity(oldHttpPost.getEntity());
            return httpPost;
        } else {
            final HttpRequestBase httpRequestBase = new HttpRequestBase() {
                @Override
                public String getMethod() {
                    return method;
                }
            };
            httpRequestBase.setURI(URI.create(uriNew));
            return httpRequestBase;
        }
    }

    @Override
    public ExecuteContext after(ExecuteContext context) throws Exception {
        return context;
    }

    @Override
    public ExecuteContext onThrow(ExecuteContext context) {
        return context;
    }
}
