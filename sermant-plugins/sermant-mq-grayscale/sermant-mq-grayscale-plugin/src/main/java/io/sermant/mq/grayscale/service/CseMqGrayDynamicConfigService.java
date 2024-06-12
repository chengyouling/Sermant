package io.sermant.mq.grayscale.service;

import io.sermant.core.plugin.service.PluginService;
import io.sermant.core.plugin.subscribe.ConfigSubscriber;
import io.sermant.core.plugin.subscribe.CseGroupConfigSubscriber;
import io.sermant.mq.grayscale.config.CseMqGrayConfigListener;

public class CseMqGrayDynamicConfigService implements PluginService {
    public CseMqGrayDynamicConfigService() {
    }

    @Override
    public void start() {
        CseMqGrayConfigListener listener = new CseMqGrayConfigListener();
        ConfigSubscriber subscriber = new CseGroupConfigSubscriber("default", listener, "SERMANT-MQ-GRAYSCALE");
        subscriber.subscribe();
    }
}
