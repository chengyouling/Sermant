package com.huawei.discovery.service.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.cloud.consul.ConsulProperties;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryClient;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;

import com.ecwid.consul.v1.ConsulClient;
import com.huawei.discovery.consul.service.DiscoveryClientService;
import com.huawei.discovery.consul.utils.HttpConstants;
import com.huawei.discovery.service.config.ConsulDiscoveryConfig;
import com.huawei.discovery.service.service.ConsulServerRefreshTask;
import com.huawei.discovery.service.service.ConsulServersWatch;
import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.plugin.config.PluginConfigManager;


/**
 * 针对http、okhttp请求方式，从注册中心获取实例列表客户端
 *
 * @author chengyouling
 * @since 2022-9-14
 */
public class HttpConsulDiscoveryClient implements DiscoveryClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    private ConsulDiscoveryClient client;

    private Map<String, Integer> serviceIndexs = new HashMap<String, Integer>();

    private ConsulServersWatch consulServersWatch;

    private ConsulServerRefreshTask consulServerRefreshTask;

    private ConsulDiscoveryConfig config = PluginConfigManager.getPluginConfig(ConsulDiscoveryConfig.class);

    @Override
    public Map<String, String> getInstances(String serviceName) {
        ServiceInstance instance = getCacheInstances(serviceName);
        Map<String, String> map = new HashMap<String, String>();
        if (instance != null) {
            map.put(HttpConstants.HTTP_URI_HOST, instance.getHost());
            map.put(HttpConstants.HTTP_URI_PORT, String.valueOf(instance.getPort()));
        }
        return map;
    }

    private ServiceInstance getCacheInstances(String serviceName) {
        List<ServiceInstance> instances = new ArrayList<ServiceInstance>();
        if (ConsulServerRefreshTask.cacheInstances.size() != 0) {
            List<ServiceInstance> cacheInstances = new ArrayList<ServiceInstance>();
            ConsulServerRefreshTask.cacheInstances.forEach(instance -> {
                if (instance.getServiceId().equals(serviceName))
                    cacheInstances.add(instance);
            });
            instances = cacheInstances;

        }
        if (instances.size() == 0) {
            instances = consulServerRefreshTask.getInstances(serviceName);
        }
        return chooseInstance(serviceName, instances);
    }

    /**
     * 选择服务实例
     * @param serviceName
     * @param instances
     * @return
     */
    private ServiceInstance chooseInstance(String serviceName, List<ServiceInstance> instances) {
        if (instances.size() == 0) {
            return null;
        }
        if (instances.size() == 1) {
            return instances.get(0);
        }
        Integer index = serviceIndexs.get(serviceName);
        if (index == null) {
            index = 0;
        }
        int currentIndex = index % instances.size();
        index++;
        serviceIndexs.put(serviceName, index);
        return instances.get(currentIndex);
    }

    @Override
    public void start() {
        if (client == null) {
            //设置consul服务参数
            ConsulProperties consulProperties = new ConsulProperties();
            consulProperties.setHost(config.getHost());
            consulProperties.setPort(config.getPort());
            consulProperties.setScheme(config.getScheme());
            ConsulClient consulClient = HttpConsulClient.getHttpConsulClient(consulProperties);
            InetUtilsProperties property = new InetUtilsProperties();
            InetUtils inetUtils = new InetUtils(property);
            //设置consul服务发现参数
            ConsulDiscoveryProperties consulDiscoveryProperties = new ConsulDiscoveryProperties(inetUtils);
            consulDiscoveryProperties.setCatalogServicesWatchTimeout(config.getCatalogServicesWatchTimeout());
            consulDiscoveryProperties.setCatalogServicesWatchDelay(config.getCatalogServicesWatchDelay());
            consulDiscoveryProperties.setAclToken(config.getAclToken());
            client = new ConsulDiscoveryClient(consulClient, consulDiscoveryProperties);
            consulServerRefreshTask = new ConsulServerRefreshTask(client, consulClient, consulDiscoveryProperties, config.getRefreshIntervalInMillis());
            consulServersWatch = new ConsulServersWatch(consulDiscoveryProperties, consulClient, consulServerRefreshTask);
        }
        LOGGER.info("consul discovery start");
    }

    @Override
    public void stop() {
        consulServersWatch.stop();
        ConsulServerRefreshTask.cacheInstances.clear();
        LOGGER.info("stop");
    }
}
