package it.agilelab.witboost.datacatalogplugin.collibra.service;

import io.vavr.Tuple2;
import it.agilelab.witboost.datacatalogplugin.collibra.config.CollibraConfig;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.Column;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.DataProduct;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.OutputPort;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface CollibraAttributeMapper {

    Logger logger = LoggerFactory.getLogger(CollibraAttributeMapper.class);

    default List<Tuple2<UUID, String>> mapColumnAttributes(CollibraConfig collibraConfig, Column column) {
        return mapAttributesF(collibraConfig.assets().column().attributes(), column::getStringFromJsonPath);
    }

    default List<Tuple2<UUID, String>> mapOutputPortAttributes(
            CollibraConfig collibraConfig, OutputPort<?> outputPort) {
        return mapAttributesF(collibraConfig.assets().outputPort().attributes(), outputPort::getStringFromJsonPath);
    }

    default List<Tuple2<UUID, String>> mapDataProductAttributes(
            CollibraConfig collibraConfig, DataProduct dataProduct) {
        return mapAttributesF(collibraConfig.assets().dataProduct().attributes(), dataProduct::getStringFromJsonPath);
    }

    private List<Tuple2<UUID, String>> mapAttributesF(
            Map<String, String> attributeMappingConfig, Function<String, String> getStringFromJsonPath) {
        return attributeMappingConfig.entrySet().stream()
                .map(attributeEntry -> {
                    var jsonPath = attributeEntry.getKey();
                    var attributeTypeId = attributeEntry.getValue();
                    logger.debug("Retrieving value on path '{}' to associate with ID '{}'", jsonPath, attributeTypeId);
                    var value = getStringFromJsonPath.apply(jsonPath);
                    logger.debug("Value on path '{}' found an it equals '{}'", jsonPath, value);
                    return new Tuple2<>(UUID.fromString(attributeTypeId), value);
                })
                .toList();
    }
}
