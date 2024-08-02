package io.sermant.mq.grayscale.interceptor;

import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.core.plugin.agent.interceptor.AbstractInterceptor;
import io.sermant.core.utils.ReflectUtils;
import io.sermant.core.utils.StringUtils;
import io.sermant.mq.grayscale.service.MqConsumerClientConfig;
import io.sermant.mq.grayscale.service.MqConsumerGroupAutoCheck;
import io.sermant.mq.grayscale.utils.MqGrayscaleConfigUtils;

import org.apache.rocketmq.client.consumer.DefaultLitePullConsumer;
import org.apache.rocketmq.client.impl.consumer.DefaultLitePullConsumerImpl;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;

import java.util.Optional;

public class MqLitePullConsumerSubscribeInterceptor extends AbstractInterceptor {
    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        return context;
    }

    @Override
    public ExecuteContext after(ExecuteContext context) throws Exception {
        if (MqGrayscaleConfigUtils.isPlugEnabled()) {
            DefaultLitePullConsumerImpl litePullConsumerImpl = (DefaultLitePullConsumerImpl) context.getObject();
            DefaultLitePullConsumer pullConsumer = litePullConsumerImpl.getDefaultLitePullConsumer();
            String baseGroup = pullConsumer.getConsumerGroup();
            String grayEnv = MqGrayscaleConfigUtils.getGrayGroupTag();
            if (StringUtils.isEmpty(grayEnv)) {
                MqConsumerClientConfig config = new MqConsumerClientConfig();
                config.setTopic((String) context.getArguments()[0]);
                Optional<Object> instance = ReflectUtils.getFieldValue(context.getObject(), "mQClientFactory");
                instance.ifPresent(o -> config.setMqClientInstance((MQClientInstance) o));
                config.setConsumerGroup(baseGroup);
                config.setAddress(pullConsumer.getNamesrvAddr());
                MqConsumerGroupAutoCheck.setConsumerClientConfig(config);
            } else {
                // consumerGroup的规则 ^[%|a-zA-Z0-9_-]+$
                String grayConsumerGroup = baseGroup.contains(grayEnv) ? baseGroup : baseGroup + "_" + grayEnv;
                pullConsumer.setConsumerGroup(grayConsumerGroup);
            }
        }
        return context;
    }
}
