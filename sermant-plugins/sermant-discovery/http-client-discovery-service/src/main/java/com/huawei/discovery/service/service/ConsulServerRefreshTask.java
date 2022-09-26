package com.huawei.discovery.service.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.servicecomb.http.client.task.AbstractTask;
import org.apache.servicecomb.http.client.task.Task;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryClient;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.model.Check;
import com.huaweicloud.sermant.core.common.LoggerFactory;

/**
 * 定时刷新consul中心实例到本地
 *
 * @author chengyouling
 * @since 2022-09-21
 */
public class ConsulServerRefreshTask extends AbstractTask {

    private final ConsulDiscoveryClient client;

    private final ConsulClient consul;

    private final ConsulDiscoveryProperties properties;

    private final long refreshIntervalInMillis;

    private static final Logger LOGGER = LoggerFactory.getLogger();

    public static volatile Set<ServiceInstance> cacheInstances = new HashSet<ServiceInstance>();

    public ConsulServerRefreshTask(ConsulDiscoveryClient client, ConsulClient consul, ConsulDiscoveryProperties properties, long refreshIntervalInMillis) {
        super("consul-server-refresh-task");
        this.client = client;
        this.consul = consul;
        this.properties = properties;
        this.refreshIntervalInMillis = refreshIntervalInMillis;
        startTask(new PollConfigurationTask(0));
    }

    public void buildLocalInstanceCache() {
        List<ServiceInstance> instances = client.getAllInstances();
        Response<List<Check>> healthChecksState = consul
                .getHealthChecksState(new QueryParams(properties.getCatalogServicesWatchTimeout(), -1));
        List<Check> checks = healthChecksState.getValue();
        instances.forEach(instance -> {
            checks.forEach(check -> {
                if (instance.getInstanceId().equals(check.getServiceId()) && StringUtils.isEmpty(check.getOutput())) {
                    cacheInstances.add(instance);
                }
            });
        });
    }

    public List<ServiceInstance> getInstances(String serviceName) {
        List<ServiceInstance> resultList = new ArrayList<ServiceInstance>();
        List<ServiceInstance> instances = client.getInstances(serviceName);
        Response<List<Check>> healthChecksState = consul
                .getHealthChecksState(new QueryParams(properties.getCatalogServicesWatchTimeout(), -1));
        List<Check> checks = healthChecksState.getValue();
        instances.forEach(instance -> {
            checks.forEach(check -> {
                if (instance.getInstanceId().equals(check.getServiceId()) && StringUtils.isEmpty(check.getOutput())) {
                    resultList.add(instance);
                }
            });
        });
        return resultList;
    }

    class PollConfigurationTask implements Task {
        final int failCount;

        public PollConfigurationTask(int failCount) {
            this.failCount = failCount;
        }

        @Override
        public void execute() {
            try {
                buildLocalInstanceCache();
                startTask(new BackOffSleepTask(refreshIntervalInMillis, new PollConfigurationTask(0)));
            } catch (Exception e) {
                LOGGER.warning("get servers from consul failed, and will try again.");
                startTask(new BackOffSleepTask(failCount + 1, new PollConfigurationTask(failCount + 1)));
            }
        }
    }
}
