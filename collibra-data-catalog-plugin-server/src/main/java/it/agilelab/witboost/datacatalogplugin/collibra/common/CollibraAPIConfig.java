package it.agilelab.witboost.datacatalogplugin.collibra.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "api")
public record CollibraAPIConfig(String username, String password, String basePath) {
    // TODO rename baseUrl to endpoint
}
