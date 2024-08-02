package io.sermant.mq.grayscale.service;

import org.apache.rocketmq.client.impl.factory.MQClientInstance;

public class MqConsumerClientConfig {
    private String topic;

    private String address;

    private String consumerGroup;

    private MQClientInstance mqClientInstance;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public MQClientInstance getMqClientInstance() {
        return mqClientInstance;
    }

    public void setMqClientInstance(MQClientInstance mqClientInstance) {
        this.mqClientInstance = mqClientInstance;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }
}
