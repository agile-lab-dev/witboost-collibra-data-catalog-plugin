package it.agilelab.witboost.datacatalogplugin.collibra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "custom-url-picker.cors")
public record CorsConfig(String endpointMask, String origin) {}
