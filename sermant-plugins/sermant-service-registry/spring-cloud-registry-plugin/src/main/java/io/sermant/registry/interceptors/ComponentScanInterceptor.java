package io.sermant.registry.interceptors;

import io.sermant.core.common.LoggerFactory;
import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.core.plugin.agent.interceptor.Interceptor;
import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.registry.config.SpecialRemoveConfig;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.logging.Logger;

public class ComponentScanInterceptor implements Interceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public ExecuteContext before(ExecuteContext context) {
        ClassPathBeanDefinitionScanner scanner = (ClassPathBeanDefinitionScanner) context.getObject();
        SpecialRemoveConfig config = PluginConfigManager.getPluginConfig(SpecialRemoveConfig.class);
        String[] componentBean = config.getComponentName().split(",");
        for (String className : componentBean) {
            try {
                Class<?> clazz = Class.forName(className);
                scanner.addExcludeFilter(new AssignableTypeFilter(clazz));
            } catch (ClassNotFoundException e) {
                LOGGER.warning("ComponentScanInterceptor can not find class: " + className);
            }
        }
        return context;
    }

    @Override
    public ExecuteContext after(ExecuteContext context) throws Exception {
        return context;
    }

    @Override
    public ExecuteContext onThrow(ExecuteContext context) throws Exception {
        return context;
    }
}
