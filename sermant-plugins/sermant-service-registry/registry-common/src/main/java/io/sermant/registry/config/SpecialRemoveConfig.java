package io.sermant.registry.config;

import io.sermant.core.config.common.ConfigTypeKey;
import io.sermant.core.plugin.config.PluginConfig;

@ConfigTypeKey(value = "remove.bean")
public class SpecialRemoveConfig implements PluginConfig {
    private String autoName;

    private String componentName;

    public String getAutoName() {
        return autoName;
    }

    public void setAutoName(String autoName) {
        this.autoName = autoName;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }
}
