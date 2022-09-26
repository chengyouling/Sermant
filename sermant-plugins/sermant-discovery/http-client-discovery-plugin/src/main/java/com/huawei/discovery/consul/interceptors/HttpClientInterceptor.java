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

import java.net.URI;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.http.client.methods.HttpUriRequest;
import com.huawei.discovery.consul.service.DiscoveryClientService;
import com.huawei.discovery.consul.utils.HttpConstants;
import com.huawei.discovery.consul.utils.HttpParamersUtils;
import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.plugin.agent.entity.ExecuteContext;
import com.huaweicloud.sermant.core.plugin.agent.interceptor.Interceptor;
import com.huaweicloud.sermant.core.service.ServiceManager;

/**
 * 拦截获取服务列表
 *
 * @author chengyouling
 * @since 2022-9-14
 */
public class HttpClientInterceptor implements Interceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    private final DiscoveryClientService discoveryClientService;

    /**
     * 构造方法
     */
    public HttpClientInterceptor() {
        discoveryClientService = ServiceManager.getService(DiscoveryClientService.class);
    }

    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        HttpUriRequest httpUriRequest = (HttpUriRequest) context.getArguments()[0];
        String method = httpUriRequest.getMethod();
        URI uri = httpUriRequest.getURI();
        if (HttpConstants.isRealmHost(uri.getHost())) {
            Map<String, String> hostAndPath = HttpParamersUtils.recoverHostAndPath(uri.getPath());
            String serviceName = hostAndPath.get(HttpConstants.HTTP_URI_HOST);
            Map<String, String> result = discoveryClientService.getInstances(serviceName);
            if (result.size() > 0) {
                String uriNew = HttpParamersUtils.buildNewUrl(uri, result, hostAndPath.get(HttpConstants.HTTP_URI_PATH), method);
                context.getArguments()[0] = HttpParamersUtils.builNewRequest(uriNew, method, httpUriRequest);
            }
        }
        return context;
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
