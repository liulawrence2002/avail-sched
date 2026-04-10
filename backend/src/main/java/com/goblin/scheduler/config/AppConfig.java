package com.goblin.scheduler.config;

import com.goblin.scheduler.web.RateLimitFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

  @Bean
  CacheManager cacheManager() {
    return new ConcurrentMapCacheManager("results");
  }

  @Bean
  WebMvcConfigurer webMvcConfigurer(AppProperties properties) {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry
            .addMapping("/api/**")
            .allowedOrigins(properties.cors().allowedOrigins().toArray(String[]::new))
            .allowedMethods("GET", "POST", "PUT", "OPTIONS")
            .allowedHeaders("*");
        registry
            .addMapping("/actuator/**")
            .allowedOrigins(properties.cors().allowedOrigins().toArray(String[]::new))
            .allowedMethods("GET")
            .allowedHeaders("*");
        registry
            .addMapping("/swagger-ui/**")
            .allowedOrigins(properties.cors().allowedOrigins().toArray(String[]::new))
            .allowedMethods("GET")
            .allowedHeaders("*");
      }
    };
  }

  @Bean
  OncePerRequestFilter rateLimitFilter() {
    return new RateLimitFilter();
  }
}
