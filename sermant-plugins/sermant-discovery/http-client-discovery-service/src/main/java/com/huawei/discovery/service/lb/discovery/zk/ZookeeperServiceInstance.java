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

package com.huawei.discovery.service.lb.discovery.zk;

import java.util.Map;

/**
 * zookeeper实例
 *
 * @author zhouss
 * @since 2022-09-26
 */
public class ZookeeperServiceInstance {
    private String id;

    private String serviceName;

    private Map<String, String> metadata;

    /**
     * 实例
     */
    public ZookeeperServiceInstance() {
    }

    /**
     * zk实例
     *
     * @param id 注册时的随机码
     * @param serviceName 服务名
     * @param metadata 元数据
     */
    public ZookeeperServiceInstance(String id, String serviceName, Map<String, String> metadata) {
        this.id = id;
        this.serviceName = serviceName;
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
