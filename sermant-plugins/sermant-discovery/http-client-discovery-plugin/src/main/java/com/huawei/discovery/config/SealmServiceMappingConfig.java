/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.discovery.config;


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
