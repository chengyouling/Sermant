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
import java.util.Optional;
import java.util.logging.Logger;

import com.huawei.discovery.consul.entity.ServiceInstance;
import com.huawei.discovery.consul.service.LbService;
import com.huawei.discovery.consul.utils.HttpConstants;
import com.huawei.discovery.consul.utils.HttpParamersUtils;
import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.plugin.agent.entity.ExecuteContext;
import com.huaweicloud.sermant.core.plugin.agent.interceptor.Interceptor;
import com.huaweicloud.sermant.core.service.ServiceManager;

import okhttp3.HttpUrl;
import okhttp3.Request;

/**
 * 拦截获取服务列表
 *
 * @author chengyouling
 * @since 2022-9-14
 */
public class OkHttpClientInterceptor implements Interceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    private final LbService lbService;

    /**
     * 构造方法
     */
    public OkHttpClientInterceptor() {
        lbService = ServiceManager.getService(LbService.class);
    }

    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        Request request = (Request)context.getRawMemberFieldValue("originalRequest");
        URI uri = request.url().uri();
        String method = request.method();
        Map<String, String> hostAndPath = HttpParamersUtils.recoverHostAndPath(uri.getPath());
        String serviceName = hostAndPath.get(HttpConstants.HTTP_URI_HOST);
        Optional<ServiceInstance> optional = lbService.choose(serviceName);
        if (optional.isPresent()) {
            String url = HttpParamersUtils.buildNewUrl(uri, optional.get(), hostAndPath.get(HttpConstants.HTTP_URI_PATH), method);
            HttpUrl newUrl = HttpUrl.parse(url);
            Request newRequest = request
                    .newBuilder()
                    .url(newUrl)
                    .build();
            context.setRawMemberFieldValue("originalRequest", newRequest);
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
