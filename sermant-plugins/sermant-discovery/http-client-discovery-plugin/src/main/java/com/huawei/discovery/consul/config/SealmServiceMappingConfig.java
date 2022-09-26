package com.huawei.discovery.consul.config;


import com.huaweicloud.sermant.core.config.common.ConfigTypeKey;
import com.huaweicloud.sermant.core.plugin.config.PluginConfig;

/**
 * 域名服务名映射配置类
 *
 * @author chengyouling
 * @since 2022-09-26
 */
@ConfigTypeKey("sealm.service.mapping.plugin")
public class SealmServiceMappingConfig implements PluginConfig {

    /**
     * 是否使用动态配置
     */
    private boolean useDynamicConfig = false;

    /**
     * 当前服务名称
     */
    private String serviceName;

    public boolean isUseDynamicConfig() {
        return useDynamicConfig;
    }

    public void setUseDynamicConfig(boolean useDynamicConfig) {
        this.useDynamicConfig = useDynamicConfig;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
