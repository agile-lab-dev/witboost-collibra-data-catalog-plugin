package it.agilelab.witboost.datacatalogplugin.collibra.config;

import java.util.Map;

public record CollibraAssetsConfig(
        DataProductConfig dataProduct, OutputPortConfig outputPort, ColumnConfig column, AssetConfig businessTerm) {
    public record DataProductConfig(
            String typeId, String containsOutputPortRelationId, Map<String, String> attributes) {}

    public record OutputPortConfig(String typeId, String containsColumnRelationId, Map<String, String> attributes) {}

    public record ColumnConfig(String typeId, Map<String, String> attributes) {}
}
