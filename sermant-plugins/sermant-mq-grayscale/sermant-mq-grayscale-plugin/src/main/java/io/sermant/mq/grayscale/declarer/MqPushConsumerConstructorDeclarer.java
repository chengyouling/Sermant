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

package io.sermant.mq.grayscale.declarer;

import io.sermant.core.plugin.agent.declarer.InterceptDeclarer;
import io.sermant.core.plugin.agent.matcher.ClassMatcher;
import io.sermant.core.plugin.agent.matcher.MethodMatcher;
import io.sermant.mq.grayscale.interceptor.MqPullConsumerConstructorInterceptor;

public class MqPushConsumerConstructorDeclarer extends MqAbstractDeclarer {
    private static final String ENHANCE_CLASS = "org.apache.rocketmq.client.consumer.DefaultMQPushConsumer";

    private static final String[] METHOD_PARAM_TYPES_1 = {
            "java.lang.String",
            "org.apache.rocketmq.remoting.RPCHook",
            "org.apache.rocketmq.client.consumer.AllocateMessageQueueStrategy"
    };

    private static final String[] METHOD_PARAM_TYPES_2 = {
            "java.lang.String",
            "java.lang.String",
            "org.apache.rocketmq.remoting.RPCHook",
            "org.apache.rocketmq.client.consumer.AllocateMessageQueueStrategy"
    };

    private static final String[] METHOD_PARAM_TYPES_3 = {
            "java.lang.String",
            "org.apache.rocketmq.remoting.RPCHook",
            "org.apache.rocketmq.client.consumer.AllocateMessageQueueStrategy",
            "boolean",
            "java.lang.String"
    };

    private static final String[] METHOD_PARAM_TYPES_4 = {
            "java.lang.String",
            "java.lang.String",
            "org.apache.rocketmq.remoting.RPCHook",
            "org.apache.rocketmq.client.consumer.AllocateMessageQueueStrategy",
            "boolean",
            "java.lang.String"
    };

    @Override
    public ClassMatcher getClassMatcher() {
        return ClassMatcher.nameEquals(ENHANCE_CLASS);
    }

    @Override
    public InterceptDeclarer[] getInterceptDeclarers(ClassLoader classLoader) {
        return new InterceptDeclarer[]{
                InterceptDeclarer.build(MethodMatcher.isConstructor()
                                .and(MethodMatcher.paramTypesEqual(METHOD_PARAM_TYPES_1)
                                        .or(MethodMatcher.paramTypesEqual(METHOD_PARAM_TYPES_2))
                                        .or(MethodMatcher.paramTypesEqual(METHOD_PARAM_TYPES_3))
                                        .or(MethodMatcher.paramTypesEqual(METHOD_PARAM_TYPES_4))),
                        new MqPullConsumerConstructorInterceptor())
        };
    }
}
