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

import com.huawei.discovery.entity.PlugEffectStategyCache;
import com.huaweicloud.sermant.core.utils.StringUtils;

/**
 * 插件生效、日志打印动态配置相关常量
 *
 * @author chengyouling
 * @since 2022-10-9
 */
public class PlugEffectWhiteBlackConstants {

    /**
     * 监听配置key
     */
    public static final String DYNAMIC_CONFIG_LISTENER_KEY = "sermant.plugin.discovery";

    /**
     * 插件生效--策略
     */
    public static final String DYNAMIC_CONFIG_STRATEGY = "strategy";

    /**
     * 插件生效--服务名白名单value
     */
    public static final String DYNAMIC_CONFIG__VALUE = "value";

    /**
     * 是否打印插件生效统计日志key
     */
    public static final String DYNAMIC_CONFIG_LOGGER = "logger";

    /**
     * 策略-所有服务生效
     */
    public static final String STRATEGY_ALL = "all";

    /**
     * 策略-所有服务不生效
     */
    public static final String STRATEGY_NONE = "none";

    /**
     * 策略-白名单服务生效
     */
    public static final String STRATEGY_WHITE = "white";

    /**
     * 策略-黑名单服务不生效
     */
    public static final String STRATEGY_BLACK = "black";

    /**
     * 是否打印插件生效统计日志
     */
    public static final String LOGGER_OPEN_FLAG = "1";

    public static boolean isPlugEffect(String serviceName) {
        String strategy = PlugEffectStategyCache.INSTANCE.getConfigContent(DYNAMIC_CONFIG_STRATEGY);
        String value = PlugEffectStategyCache.INSTANCE.getConfigContent(DYNAMIC_CONFIG__VALUE);
        //全部生效
        if (StringUtils.equalsIgnoreCase(STRATEGY_ALL, strategy)) {
            return true;
        }
        //全部不生效
        if (StringUtils.equalsIgnoreCase(STRATEGY_NONE, strategy)) {
            return false;
        }
        //白名单-插件生效
        if (StringUtils.equalsIgnoreCase(STRATEGY_WHITE, strategy)) {
            if (value.contains(serviceName)) {
                return true;
            } else {
                return false;
            }
        }
        //黑名单-插件不生效
        if (StringUtils.equalsIgnoreCase(STRATEGY_BLACK, strategy)) {
            if (value.contains(serviceName)) {
                return false;
            } else {
                return true;
            }
        }
        return true;
    }

    public static boolean isOpenLogger() {
        String value = PlugEffectStategyCache.INSTANCE.getConfigContent(DYNAMIC_CONFIG_LOGGER);
        return StringUtils.equalsIgnoreCase(LOGGER_OPEN_FLAG, value);
    }
}
