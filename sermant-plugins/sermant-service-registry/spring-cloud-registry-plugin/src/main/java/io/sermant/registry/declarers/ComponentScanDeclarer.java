package io.sermant.registry.declarers;

import io.sermant.core.plugin.agent.declarer.InterceptDeclarer;
import io.sermant.core.plugin.agent.matcher.ClassMatcher;
import io.sermant.core.plugin.agent.matcher.MethodMatcher;
import io.sermant.registry.interceptors.ComponentScanInterceptor;

public class ComponentScanDeclarer extends AbstractBaseConfigDeclarer {
    private static final String ENHANCE_CLASS = "org.springframework.context.annotation.ClassPathBeanDefinitionScanner";

    private static final String INTERCEPTOR_CLASS = ComponentScanInterceptor.class.getCanonicalName();

    @Override
    public ClassMatcher getClassMatcher() {
        return ClassMatcher.nameEquals(ENHANCE_CLASS);
    }

    @Override
    public InterceptDeclarer[] getInterceptDeclarers(ClassLoader classLoader) {
        return new InterceptDeclarer[] {
                InterceptDeclarer.build(MethodMatcher.nameEquals("doScan"), INTERCEPTOR_CLASS)
        };
    }

    @Override
    public boolean isEnabled() {
        return isEnableSpringRegistry();
    }
}
