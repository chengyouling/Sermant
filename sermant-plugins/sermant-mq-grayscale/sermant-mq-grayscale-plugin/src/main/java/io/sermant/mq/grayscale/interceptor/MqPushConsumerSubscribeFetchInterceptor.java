package io.sermant.mq.grayscale.interceptor;

import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.core.plugin.agent.interceptor.AbstractInterceptor;
import io.sermant.core.utils.ReflectUtils;
import io.sermant.core.utils.StringUtils;
import io.sermant.mq.grayscale.service.MqConsumerClientConfig;
import io.sermant.mq.grayscale.service.MqConsumerGroupAutoCheck;
import io.sermant.mq.grayscale.utils.MqGrayscaleConfigUtils;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.impl.consumer.DefaultMQPushConsumerImpl;

public class MqPushConsumerSubscribeFetchInterceptor extends AbstractInterceptor {
    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        return context;
    }

    @Override
    public ExecuteContext after(ExecuteContext context) throws Exception {
        if (MqGrayscaleConfigUtils.isPlugEnabled()) {
            DefaultMQPushConsumerImpl pushConsumerImpl = (DefaultMQPushConsumerImpl) context.getObject();
            DefaultMQPushConsumer pushConsumer = pushConsumerImpl.getDefaultMQPushConsumer();
            String baseGroup = pushConsumer.getConsumerGroup();
            String grayEnv = MqGrayscaleConfigUtils.getGrayGroupTag();
            if (StringUtils.isEmpty(grayEnv)) {
                MqConsumerClientConfig config = new MqConsumerClientConfig();
                config.setTopic((String) context.getArguments()[0]);
                config.setMqClientInstance(pushConsumerImpl.getmQClientFactory());
                config.setConsumerGroup(baseGroup);
                config.setAddress(pushConsumer.getNamesrvAddr());
                MqConsumerGroupAutoCheck.setConsumerClientConfig(config);
            } else {
                // consumerGroup的规则 ^[%|a-zA-Z0-9_-]+$
                String grayConsumerGroup = baseGroup.contains(grayEnv) ? baseGroup : baseGroup + "_" + grayEnv;
                pushConsumer.setConsumerGroup(grayConsumerGroup);
            }
        }
        return context;
    }
}
