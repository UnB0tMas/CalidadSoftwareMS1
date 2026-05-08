package com.upsjb.ms1.config;

import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ObjectProvider<HandlerMethodArgumentResolver> customArgumentResolvers;

    public WebMvcConfig(ObjectProvider<HandlerMethodArgumentResolver> customArgumentResolvers) {
        this.customArgumentResolvers = customArgumentResolvers;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        List<HandlerMethodArgumentResolver> orderedResolvers = customArgumentResolvers
                .orderedStream()
                .sorted(AnnotationAwareOrderComparator.INSTANCE)
                .toList();

        for (HandlerMethodArgumentResolver resolver : orderedResolvers) {
            if (!resolvers.contains(resolver)) {
                resolvers.add(resolver);
            }
        }
    }
}