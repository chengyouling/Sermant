package com.huawei.discovery.service.client;

import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.consul.ConsulProperties;

import com.ecwid.consul.transport.TLSConfig;
import com.ecwid.consul.v1.ConsulClient;

/**
 * 构建consul客户端
 *
 * @author chengyouling
 * @since 2022-9-14
 */
public class HttpConsulClient {

    public static ConsulClient getHttpConsulClient(ConsulProperties consulProperties) {
        final int agentPort = consulProperties.getPort();
        final String agentHost = !StringUtils.isEmpty(consulProperties.getScheme())
                ? consulProperties.getScheme() + "://" + consulProperties.getHost()
                : consulProperties.getHost();
        if (consulProperties.getTls() != null) {
            ConsulProperties.TLSConfig tls = consulProperties.getTls();
            TLSConfig tlsConfig = new TLSConfig(tls.getKeyStoreInstanceType(),
                    tls.getCertificatePath(), tls.getCertificatePassword(),
                    tls.getKeyStorePath(), tls.getKeyStorePassword());
            return new ConsulClient(agentHost, agentPort, tlsConfig);
        }
        return new ConsulClient(agentHost, agentPort);
    }
}
