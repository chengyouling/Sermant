package io.sermant.mq.grayscale.declarer;

import io.sermant.core.plugin.agent.declarer.InterceptDeclarer;
import io.sermant.core.plugin.agent.matcher.ClassMatcher;
import io.sermant.core.plugin.agent.matcher.MethodMatcher;
import io.sermant.mq.grayscale.interceptor.MqPullConsumerFetchInterceptor;

public class MqPullConsumerFetchDeclarer extends MqAbstractDeclarer {
    private static final String ENHANCE_CLASS = "org.apache.rocketmq.client.impl.consumer.DefaultMQPullConsumerImpl";

    private static final String METHOD_NAME_SUB = "fetchSubscribeMessageQueues";

    private static final String METHOD_NAME_BALANCE = "fetchMessageQueuesInBalance";

    @Override
    public ClassMatcher getClassMatcher() {
        return ClassMatcher.nameEquals(ENHANCE_CLASS);
    }

    @Override
    public InterceptDeclarer[] getInterceptDeclarers(ClassLoader classLoader) {
        return new InterceptDeclarer[]{
                InterceptDeclarer.build(MethodMatcher.nameEquals(METHOD_NAME_BALANCE)
                        .or(MethodMatcher.nameEquals(METHOD_NAME_SUB)), new MqPullConsumerFetchInterceptor())
        };
    }
}
