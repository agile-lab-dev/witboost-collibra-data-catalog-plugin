package it.agilelab.witboost.datacatalogplugin.collibra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "api")
public record CollibraAPIConfig(String username, String password, String endpoint) {}
