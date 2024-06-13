package io.sermant.mq.grayscale.interceptor;

import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.core.plugin.agent.interceptor.AbstractInterceptor;
import io.sermant.core.utils.StringUtils;
import io.sermant.mq.grayscale.service.MqConsumerGroupAutoCheck;
import io.sermant.mq.grayscale.utils.MqGrayscaleConfigUtils;
import io.sermant.mq.grayscale.utils.SubscriptionDataUtils;

import org.apache.rocketmq.common.protocol.heartbeat.SubscriptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubscriptionDataUpdateInterceptor extends AbstractInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionDataUpdateInterceptor.class);

    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        return context;
    }

    @Override
    public ExecuteContext after(ExecuteContext context) throws Exception {
        if (MqGrayscaleConfigUtils.isPlugEnabled() && MqGrayscaleConfigUtils.isMqServerGrayEnabled()) {
            SubscriptionData subscriptionData = (SubscriptionData) context.getResult();
            MqConsumerGroupAutoCheck.setTopic(subscriptionData.getTopic());
            if (SubscriptionDataUtils.EXPRESSION_TYPE_TAG.equals(subscriptionData.getExpressionType())) {
                SubscriptionDataUtils.resetsSQL92SubscriptionData(subscriptionData);
            } else if (SubscriptionDataUtils.EXPRESSION_TYPE_SQL92.equals(subscriptionData.getExpressionType())) {
                String originSubData = subscriptionData.getSubString();
                String subStr = SubscriptionDataUtils.addMseGrayTagsToSQL92Expression(originSubData);
                subscriptionData.setSubString(subStr);
                LOGGER.warn("update SQL92 subscriptionData, originSubStr: {}, newSubStr: {}", originSubData, subStr);
            } else {
                LOGGER.warn("can not process expressionType: {}", subscriptionData.getExpressionType());
            }
        }
        return context;
    }
}
