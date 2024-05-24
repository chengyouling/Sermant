/*
 * Copyright (C) 2021-2022 Huawei Technologies Co., Ltd. All rights reserved.
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

package io.sermant.flowcontrol.boot3;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.core.plugin.agent.interceptor.Interceptor;
import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.core.plugin.service.PluginServiceManager;
import io.sermant.core.utils.LogUtils;
import io.sermant.flowcontrol.boot3.service.HttpRest4jService;
import io.sermant.flowcontrol.boot3.service.HttpSenService;
import io.sermant.flowcontrol.boot3.service.HttpService;
import io.sermant.flowcontrol.common.config.ConfigConst;
import io.sermant.flowcontrol.common.config.FlowControlConfig;
import io.sermant.flowcontrol.common.entity.FlowControlResult;
import io.sermant.flowcontrol.common.entity.HttpRequestEntity;
import io.sermant.flowcontrol.common.entity.RequestEntity.RequestType;
import io.sermant.flowcontrol.common.enums.FlowFramework;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The API interface of DispatcherServlet is enhanced to define sentinel resources.
 *
 * @author zhouss
 * @since 2022-02-11
 */
public class DispatcherServletInterceptorBoot3 implements Interceptor {
    private final String className = DispatcherServletInterceptorBoot3.class.getName();

    private final ReentrantLock lock = new ReentrantLock();

    private HttpService httpService;

    private final FlowControlConfig flowControlConfig;

    protected DispatcherServletInterceptorBoot3() {
        flowControlConfig = PluginConfigManager.getPluginConfig(FlowControlConfig.class);
    }

    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        LogUtils.printHttpRequestBeforePoint(context);
        final Object[] allArguments = context.getArguments();
        final HttpServletRequest argument = (HttpServletRequest) allArguments[0];
        final FlowControlResult result = new FlowControlResult();
        final HttpRequestEntity httpRequestEntity = convertToHttpEntity(argument);
        if (httpRequestEntity == null) {
            return context;
        }
        chooseHttpService().onBefore(className, httpRequestEntity, result);
        if (result.isSkip()) {
            context.skip(null);
            final HttpServletResponse response = (HttpServletResponse) allArguments[1];
            if (response != null) {
                response.setStatus(result.getResponse().getCode());
                response.getWriter().print(result.buildResponseMsg());
            }
        }
        return context;
    }

    @Override
    public ExecuteContext after(ExecuteContext context) throws Exception {
        chooseHttpService().onAfter(className, context.getResult());
        LogUtils.printHttpRequestAfterPoint(context);
        return context;
    }

    @Override
    public ExecuteContext onThrow(ExecuteContext context) throws Exception {
        chooseHttpService().onThrow(className, context.getThrowable());
        LogUtils.printHttpRequestOnThrowPoint(context);
        return context;
    }


    /**
     * http request data conversion adapts to plugin -> service data transfer Note that this method is not
     * extractable，Because host dependencies can only be loaded by this interceptor, pulling out results in classes not
     * being found.
     *
     * @param request request
     * @return HttpRequestEntity
     */
    private HttpRequestEntity convertToHttpEntity(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String uri = request.getRequestURI();
        return new HttpRequestEntity.Builder()
            .setRequestType(RequestType.SERVER)
            .setPathInfo(request.getPathInfo())
            .setServletPath(uri)
            .setHeaders(getHeaders(request))
            .setMethod(request.getMethod())
            .setServiceName(request.getHeader(ConfigConst.FLOW_REMOTE_SERVICE_NAME_HEADER_KEY))
            .build();
    }

    /**
     * gets http request header information
     *
     * @param request request information
     * @return headers
     */
    private Map<String, String> getHeaders(HttpServletRequest request) {
        final Enumeration<String> headerNames = request.getHeaderNames();
        final Map<String, String> headers = new HashMap<>();
        while (headerNames.hasMoreElements()) {
            final String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        return Collections.unmodifiableMap(headers);
    }

    /**
     * gets the selected http service
     *
     * @return HttpService
     */
    protected final HttpService chooseHttpService() {
        if (httpService == null) {
            lock.lock();
            try {
                if (flowControlConfig.getFlowFramework() == FlowFramework.SENTINEL) {
                    httpService = PluginServiceManager.getPluginService(HttpSenService.class);
                } else {
                    httpService = PluginServiceManager.getPluginService(HttpRest4jService.class);
                }
            } finally {
                lock.unlock();
            }
        }
        return httpService;
    }
}
