/*
 * Copyright (C) 2021-2021 Huawei Technologies Co., Ltd. All rights reserved.
 */

package com.lubanops.stresstest.http;

import com.huawei.javamesh.core.agent.definition.EnhanceDefinition;
import com.huawei.javamesh.core.agent.definition.MethodInterceptPoint;
import com.huawei.javamesh.core.agent.matcher.ClassMatcher;
import com.huawei.javamesh.core.agent.matcher.ClassMatchers;

import net.bytebuddy.matcher.ElementMatchers;

/**
 * httpclient 基础增强
 *
 * @author yiwei
 * @since 2021/10/25
 */
public abstract class AbstractHttpClientEnhance implements EnhanceDefinition{
    private final String enhanceClass;

    private final String interceptorClass;

    protected AbstractHttpClientEnhance(String enhanceClass, String interceptorClass) {
        this.enhanceClass = enhanceClass;
        this.interceptorClass = interceptorClass;
    }

    @Override
    public ClassMatcher enhanceClass() {
        return ClassMatchers.named(enhanceClass);
    }

    @Override
    public MethodInterceptPoint[] getMethodInterceptPoints() {
        return new MethodInterceptPoint[]{MethodInterceptPoint.newInstMethodInterceptPoint(interceptorClass,
                ElementMatchers.named("execute"))
        };
    }
}
