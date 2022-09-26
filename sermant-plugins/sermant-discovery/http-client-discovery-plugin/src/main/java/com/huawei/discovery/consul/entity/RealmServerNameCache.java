package com.huawei.discovery.consul.entity;

import java.util.HashMap;
import java.util.Map;

import com.huaweicloud.sermant.core.service.dynamicconfig.common.DynamicConfigEventType;

public enum RealmServerNameCache {

    INSTANCE;

    private String realmStr;

    private String serviceName;

    private Map<String, String> caches= new HashMap<String, String>();

    public void addCache(String realmStr, String serviceName) {
        caches.putIfAbsent(realmStr, serviceName);
    }

    public String convertToServiceName(String realmStr) {
        return caches.get(realmStr);
    }

    public void release() {
        caches.clear();
    }

    public String getRealmStr() {
        return realmStr;
    }

    public void setRealmStr(String realmStr) {
        this.realmStr = realmStr;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void resolve(DynamicConfigEventType eventType, String content) {

    }
}
