package it.agilelab.witboost.datacatalogplugin.collibra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "collibra")
public record CollibraConfig(
        String baseCommunityId, String initialStatusId, CollibraDomainsConfig domains, CollibraAssetsConfig assets) {}
