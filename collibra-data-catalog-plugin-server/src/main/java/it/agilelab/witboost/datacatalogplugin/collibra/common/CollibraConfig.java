package it.agilelab.witboost.datacatalogplugin.collibra.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "collibra")
public record CollibraConfig(
        String dataProductTypeId,
        String statusId,
        String communityId,
        String domainTypeId,
        String descriptionAttributeTypeId,
        String columnTypeId,
        String relationTypeId) {}
