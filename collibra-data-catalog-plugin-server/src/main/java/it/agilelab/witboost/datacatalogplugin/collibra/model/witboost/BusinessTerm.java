package it.agilelab.witboost.datacatalogplugin.collibra.model.witboost;

import it.agilelab.witboost.datacatalogplugin.collibra.client.model.Asset;

public record BusinessTerm(String id, String value) {
    public BusinessTerm(Asset asset) {
        this(asset.getId().toString(), asset.getDisplayName());
    }
}
