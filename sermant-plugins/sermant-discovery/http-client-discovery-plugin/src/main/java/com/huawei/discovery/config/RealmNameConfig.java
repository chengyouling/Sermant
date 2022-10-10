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
 * 域名配置
 *
 * @author chengyouling
 * @since 2022-09-27
 */
@ConfigTypeKey("sermant.discovery.realm")
public class RealmNameConfig implements PluginConfig {

    /**
     * 当前使用域名
     */
    private String currentRealmName = "gateway.t3go.com.cn";

    public String getCurrentRealmName() {
        return currentRealmName;
    }

    public void setCurrentRealmName(String currentRealmName) {
        this.currentRealmName = currentRealmName;
    }

}
