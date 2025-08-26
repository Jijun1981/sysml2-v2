package com.sysml.mvp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS配置 - 完全开放跨域访问
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")  // 允许所有源
                .allowedMethods("*")          // 允许所有方法
                .allowedHeaders("*")          // 允许所有头
                .allowCredentials(true)       // 允许凭证
                .maxAge(3600);               // 缓存时间
    }
}