package com.huawei.discovery.interceptors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.huawei.discovery.service.InvokerService;
import com.huaweicloud.sermant.core.plugin.agent.entity.ExecuteContext;
import com.huaweicloud.sermant.core.service.ServiceManager;

import okhttp3.Request;
import reactor.core.publisher.Flux;

public class OkHttp3ClientInterceptorTest extends BaseDiscoveryTest<OkHttp3ClientInterceptor> {

    private final List<String> services = new ArrayList<>();

    @Mock
    private InvokerService invokerService;

    @Override
    protected OkHttp3ClientInterceptor getInterceptor() {
        return new OkHttp3ClientInterceptor();
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        serviceManagerMockedStatic.when(() -> ServiceManager.getService(InvokerService.class))
                .thenReturn(invokerService);
        services.add("test1");
        services.add("test2");
    }

    @Test
    public void doBefore() throws Exception {
        Request request = new Request.Builder()
                .url("http://gateway.t3go.com.cn/zookeeper-provider-demo/sayHello?name=123")
                .build();
        ExecuteContext context = ExecuteContext.forMemberMethod(new Object(), null, null, null, null);
        context.setRawMemberFieldValue("originalRequest", request);
        interceptor.before(context);
        Request requestNew = (Request)context.getRawMemberFieldValue("originalRequest");
        String result = requestNew.url().uri().getHost();
        Assert.assertEquals("127.0.0.1", result);
    }
}
