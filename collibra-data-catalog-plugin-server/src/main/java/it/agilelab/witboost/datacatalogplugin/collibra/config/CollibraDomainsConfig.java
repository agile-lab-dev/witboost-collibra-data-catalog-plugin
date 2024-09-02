package it.agilelab.witboost.datacatalogplugin.collibra.config;

public record CollibraDomainsConfig(CollibraDomainConfig assetDomain, CollibraDomainConfig glossary) {

    public record CollibraDomainConfig(String name, String typeId) {}
}
