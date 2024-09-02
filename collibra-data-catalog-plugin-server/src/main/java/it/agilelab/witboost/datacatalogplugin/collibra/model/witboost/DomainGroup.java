package it.agilelab.witboost.datacatalogplugin.collibra.model.witboost;

import it.agilelab.witboost.datacatalogplugin.collibra.client.model.Community;
import it.agilelab.witboost.datacatalogplugin.collibra.client.model.Domain;

public record DomainGroup(Community rootDomain, Domain dataAssetDomain, Domain glossary) {}
