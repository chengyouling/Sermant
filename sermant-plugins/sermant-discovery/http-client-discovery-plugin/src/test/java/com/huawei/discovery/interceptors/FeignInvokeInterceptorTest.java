package com.huawei.discovery.interceptors;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.huawei.discovery.service.InvokerService;
import com.huaweicloud.sermant.core.plugin.service.PluginServiceManager;

public class FeignInvokeInterceptorTest extends BaseTest{

    @Mock
    private InvokerService invokerService;

    private final String realmName = "gateway.t3go.com.cn";

    @Override
    public void setUp() {
        super.setUp();
        MockitoAnnotations.openMocks(this);
        pluginServiceManagerMockedStatic.when(() -> PluginServiceManager.getPluginService(InvokerService.class))
                .thenReturn(invokerService);
        discoveryPluginConfig.setRealmName(realmName);
    }
}
