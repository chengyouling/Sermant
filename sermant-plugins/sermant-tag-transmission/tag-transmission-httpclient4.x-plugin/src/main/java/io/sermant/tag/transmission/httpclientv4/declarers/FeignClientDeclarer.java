/*
 * Copyright (C) 2024-2024 Sermant Authors. All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.sermant.tag.transmission.httpclientv4.declarers;

import io.sermant.core.plugin.agent.declarer.AbstractPluginDeclarer;
import io.sermant.core.plugin.agent.declarer.InterceptDeclarer;
import io.sermant.core.plugin.agent.matcher.ClassMatcher;
import io.sermant.core.plugin.agent.matcher.MethodMatcher;
import io.sermant.tag.transmission.httpclientv4.interceptors.FeignClientInterceptor;

/**
 * The client enhancement class initiates the feign request method
 *
 * @author provenceee
 * @since 2022-07-12
 */
public class FeignClientDeclarer extends AbstractPluginDeclarer {
    private static final String ENHANCE_CLASS = "feign.Client";

    private static final String METHOD_NAME = "execute";

    /**
     * Constructor
     */
    @Override
    public ClassMatcher getClassMatcher() {
        return ClassMatcher.isExtendedFrom(ENHANCE_CLASS);
    }

    @Override
    public InterceptDeclarer[] getInterceptDeclarers(ClassLoader classLoader) {
        return new InterceptDeclarer[]{
                InterceptDeclarer.build(MethodMatcher.nameEquals(METHOD_NAME),
                        new FeignClientInterceptor())
        };
    }
}