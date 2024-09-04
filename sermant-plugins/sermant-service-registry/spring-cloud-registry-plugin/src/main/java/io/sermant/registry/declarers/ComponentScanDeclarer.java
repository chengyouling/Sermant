/*
 * Copyright (C) 2023-2023 Huawei Technologies Co., Ltd. All rights reserved.
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

package io.sermant.registry.declarers;

import io.sermant.core.plugin.agent.declarer.InterceptDeclarer;
import io.sermant.core.plugin.agent.matcher.ClassMatcher;
import io.sermant.core.plugin.agent.matcher.MethodMatcher;
import io.sermant.registry.interceptors.ComponentScanInterceptor;

/**
 * 拦截ComponentScan注入自定配置源定制化处理
 *
 * @author chengyouling
 * @since 2023-01-06
 */
public class ComponentScanDeclarer extends AbstractBaseConfigDeclarer {
    private static final String ENHANCE_CLASS = "org.springframework.context.annotation.ClassPathBeanDefinitionScanner";

    private static final String INTERCEPTOR_CLASS = ComponentScanInterceptor.class.getCanonicalName();

    @Override
    public ClassMatcher getClassMatcher() {
        return ClassMatcher.nameEquals(ENHANCE_CLASS);
    }

    @Override
    public InterceptDeclarer[] getInterceptDeclarers(ClassLoader classLoader) {
        return new InterceptDeclarer[] {
                InterceptDeclarer.build(MethodMatcher.nameEquals("doScan"), INTERCEPTOR_CLASS)
        };
    }

    @Override
    public boolean isEnabled() {
        return isEnableSpringRegistry();
    }
}
