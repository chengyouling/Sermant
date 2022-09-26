package com.huawei.discovery.consul.service;

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
