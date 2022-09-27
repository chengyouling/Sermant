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

package com.huawei.discovery.consul.entity;

import java.util.Map;

/**
 * 实例
 *
 * @author zhouss
 * @since 2022-09-26
 */
public interface ServiceInstance {
    /**
     * 所属服务名
     *
     * @return 服务名
     */
    String serviceName();

    /**
     * 获取域名
     *
     * @return 域名
     */
    String getHost();

    /**
     * 获取IP地址
     *
     * @return IP
     */
    String getIp();

    /**
     * 端口
     *
     * @return port
     */
    int getPort();

    /**
     * 获取源数据
     *
     * @return metadata
     */
    Map<String, String> getMetadata();

    /**
     * 获取实例状态
     * 该数据仅当开启需记录指标数据的负载均衡才开启, 例如依据响应时间选择实例
     *
     * @return InstanceStats
     */
    InstanceStats getStats();
}
