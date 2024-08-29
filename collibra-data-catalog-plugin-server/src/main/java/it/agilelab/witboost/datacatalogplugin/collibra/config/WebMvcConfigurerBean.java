package it.agilelab.witboost.datacatalogplugin.collibra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfigurerBean {
    @Bean
    public WebMvcConfigurer corsConfigurer(CorsConfig config) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping(config.endpointMask()).allowedOrigins(config.origin());
            }
        };
    }
}
