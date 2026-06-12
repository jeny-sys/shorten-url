package com.yourname.shorten.config;

import com.yourname.shorten.controller.RateLimitFilter;
import com.yourname.shorten.infrastructure.RateLimiter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(RateLimiter rateLimiter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitFilter(rateLimiter));
        registration.addUrlPatterns("/api/shorten");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
