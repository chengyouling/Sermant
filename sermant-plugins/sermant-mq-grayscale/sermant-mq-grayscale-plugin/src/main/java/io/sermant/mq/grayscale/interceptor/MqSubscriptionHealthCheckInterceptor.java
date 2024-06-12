package io.sermant.mq.grayscale.interceptor;

import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.core.plugin.agent.interceptor.AbstractInterceptor;
import io.sermant.core.utils.StringUtils;
import io.sermant.mq.grayscale.utils.MqGrayscaleConfigUtils;
import io.sermant.mq.grayscale.utils.SubscriptionDataUtils;

import org.apache.rocketmq.common.protocol.heartbeat.SubscriptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.ConcurrentMap;

public class MqSubscriptionHealthCheckInterceptor extends AbstractInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqSubscriptionHealthCheckInterceptor.class);

    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        return context;
    }

    @Override
    public ExecuteContext after(ExecuteContext context) throws Exception {
        if (MqGrayscaleConfigUtils.isPlugEnabled()
                && MqGrayscaleConfigUtils.isMqServerGrayEnabled()
                && MqGrayscaleConfigUtils.MQ_EXCLUDE_TAGS_CHANGE_FLAG) {
            ConcurrentMap<String, SubscriptionData> map = (ConcurrentMap<String, SubscriptionData>) context.getResult();
            Iterator<SubscriptionData> iterator = map.values().iterator();
            if (iterator.hasNext()) {
                SubscriptionData subscriptionData = iterator.next();
                String originSubData;
                String subStr;
                if (SubscriptionDataUtils.EXPRESSION_TYPE_TAG.equals(subscriptionData.getExpressionType())) {
                    SubscriptionDataUtils.resetsSQL92SubscriptionData(subscriptionData);
                } else if (SubscriptionDataUtils.EXPRESSION_TYPE_SQL92.equals(subscriptionData.getExpressionType())) {
                    originSubData = subscriptionData.getSubString();
                    subStr = SubscriptionDataUtils.addMseGrayTagsToSQL92Expression(originSubData);
                    if (StringUtils.isEmpty(subStr)) {
                        subStr = "( " + MqGrayscaleConfigUtils.MICRO_SERVICE_GRAY_TAG_KEY + "  is null ) or ( "
                                + MqGrayscaleConfigUtils.MICRO_SERVICE_GRAY_TAG_KEY + "  is not null )";
                    }
                    subscriptionData.setSubString(subStr);
                    subscriptionData.setSubVersion(System.currentTimeMillis());
                    LOGGER.warn("update SQL92 subscriptionData, originSubStr: {}, newSubStr: {}", originSubData,
                            subStr);
                } else {
                    LOGGER.warn("can not process expressionType: {}", subscriptionData.getExpressionType());
                }
            }
            MqGrayscaleConfigUtils.MQ_EXCLUDE_TAGS_CHANGE_FLAG = false;
        }
        return context;
    }
}
