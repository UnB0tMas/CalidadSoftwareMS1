package com.upsjb.ms1.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@Configuration
public class CacheConfig {

    private final AppPropertiesConfig appProperties;

    public CacheConfig(AppPropertiesConfig appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public CacheManager cacheManager() {
        AppPropertiesConfig.Cache cacheProperties = appProperties.getCache();

        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(cacheProperties.getNames());
        cacheManager.setAllowNullValues(cacheProperties.isAllowNullValues());
        cacheManager.setCaffeine(buildCaffeine(cacheProperties));

        return cacheManager;
    }

    private Caffeine<Object, Object> buildCaffeine(AppPropertiesConfig.Cache cacheProperties) {
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(cacheProperties.getExpireAfterWriteMinutes()))
                .maximumSize(cacheProperties.getMaximumSize());

        if (cacheProperties.isRecordStats()) {
            caffeine.recordStats();
        }

        return caffeine;
    }
}