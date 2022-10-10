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

package com.huawei.discovery.service;

import java.util.Map;

import com.huaweicloud.sermant.core.plugin.service.PluginService;

/**
 * 服务发现类
 *
 * @author chengyouling
 * @since 2022-9-14
 */
public interface DiscoveryClientService extends PluginService {

    /**
     * 获取实例列表
     *
     * @param serviceName 服务名
     * @return 实例列表
     */
    Map<String, String> getInstances(String serviceName);
}
