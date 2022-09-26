package com.huawei.discovery.service.service;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.huaweicloud.sermant.core.common.LoggerFactory;

/**
 * 定时检查consul中心实例变化
 *
 * @author chengyouling
 * @since 2022-09-21
 */
public class ConsulServersWatch {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    private final ConsulDiscoveryProperties properties;

    private final ConsulClient consul;

    private final TaskScheduler taskScheduler;

    private final AtomicReference<BigInteger> catalogServicesIndex = new AtomicReference<>();

    private final AtomicBoolean running = new AtomicBoolean(false);

    private ScheduledFuture<?> watchFuture;

    private ConsulServerRefreshTask consulServerRefreshTask;

    public ConsulServersWatch(ConsulDiscoveryProperties properties, ConsulClient consul, ConsulServerRefreshTask consulServerRefreshTask) {
        this(properties, consul, getTaskScheduler());
        this.consulServerRefreshTask = consulServerRefreshTask;
        this.start();
    }

    public ConsulServersWatch(ConsulDiscoveryProperties properties, ConsulClient consul,
            TaskScheduler taskScheduler) {
        this.properties = properties;
        this.consul = consul;
        this.taskScheduler = taskScheduler;
    }

    private static ThreadPoolTaskScheduler getTaskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.initialize();
        taskScheduler.setThreadGroupName("consul-servers-watch-scheduler-task");
        return taskScheduler;
    }

    public void start() {
        if (this.running.compareAndSet(false, true)) {
            this.watchFuture = this.taskScheduler.scheduleWithFixedDelay(
                    this::catalogServicesWatch,
                    this.properties.getCatalogServicesWatchDelay());
        }
    }

    public void stop() {
        if (this.running.compareAndSet(true, false) && this.watchFuture != null) {
            this.watchFuture.cancel(true);
        }
    }

    public void catalogServicesWatch() {
        try {
            long index = -1;
            if (this.catalogServicesIndex.get() != null) {
                index = this.catalogServicesIndex.get().longValue();
            }
            Response<Map<String, List<String>>> response = this.consul.getCatalogServices(
                    new QueryParams(this.properties.getCatalogServicesWatchTimeout(),
                            index),
                    this.properties.getAclToken());
            Long consulIndex = response.getConsulIndex();
            if (consulIndex != null && consulIndex != this.catalogServicesIndex.get().longValue()) {
                this.catalogServicesIndex.set(BigInteger.valueOf(consulIndex));
                LOGGER.warning("Received services update from consul: " + response.getValue()
                        + ", index: " + consulIndex);
                consulServerRefreshTask.buildLocalInstanceCache();
            }
        }
        catch (Exception e) {
            LOGGER.warning("Error watching Consul CatalogServices" + e.getMessage());
        }
    }
}
