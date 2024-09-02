package it.agilelab.witboost.datacatalogplugin.collibra.config;

public record CollibraAssetsConfig(
        DataProductConfig dataProduct, OutputPortConfig outputPort, ColumnConfig column, AssetConfig businessTerm) {
    public record DataProductConfig(String typeId, String containsOutputPortRelationId, AttributesConfig attributes) {
        public record AttributesConfig(String description) {}
    }

    public record OutputPortConfig(String typeId, String containsColumnRelationId, AttributesConfig attributes) {
        public record AttributesConfig(String description, String tableType) {}
    }

    public record ColumnConfig(String typeId, AttributesConfig attributes) {
        public record AttributesConfig(String description, String dataType) {}
    }
}
