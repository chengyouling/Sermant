package io.sermant.registry.interceptors;

import io.sermant.core.common.LoggerFactory;
import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.core.plugin.agent.interceptor.Interceptor;
import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.core.utils.StringUtils;
import io.sermant.registry.config.SpecialRemoveConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SpringFactoriesVovInterceptor implements Interceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private static final String SPRING_BOOT_AUTOCONFIGURE =
            "org.springframework.boot.autoconfigure.EnableAutoConfiguration";

    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        return context;
    }

    @Override
    public ExecuteContext after(ExecuteContext context) throws Exception {
        SpecialRemoveConfig config = PluginConfigManager.getPluginConfig(SpecialRemoveConfig.class);
        String autoConfig = config.getAutoName();
        Object result = context.getResult();
        if (!StringUtils.isEmpty(autoConfig) && result instanceof Map) {
            injectConfigurations((Map<String, List<String>>)result, autoConfig);
        }
        return context;
    }

    private void injectConfigurations(Map<String, List<String>> result, String autoConfig) {
        List<String> configurations = result.get(SPRING_BOOT_AUTOCONFIGURE);
        String[] removeBeans = autoConfig.split(",");
        List<String> newConfigurations = new ArrayList<String>(configurations);
        for (String str : removeBeans) {
            if (configurations != null && configurations.contains(str)) {
                LOGGER.warning("find volvo consul retry class: " + str);
                newConfigurations.remove(str);
            }
        }
        result.put(SPRING_BOOT_AUTOCONFIGURE, newConfigurations);
    }

    @Override
    public ExecuteContext onThrow(ExecuteContext context) throws Exception {
        return context;
    }
}
