package it.agilelab.witboost.datacatalogplugin.collibra.service;

import it.agilelab.witboost.datacatalogplugin.collibra.common.DataCatalogPluginValidationException;
import it.agilelab.witboost.datacatalogplugin.collibra.common.FailedOperation;
import it.agilelab.witboost.datacatalogplugin.collibra.common.Problem;
import it.agilelab.witboost.datacatalogplugin.collibra.config.CollibraConfig;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.DataProduct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CollibraValidatorService implements ValidatorService, CollibraAttributeMapper {

    private static final Logger logger = LoggerFactory.getLogger(CollibraValidatorService.class);

    private final CollibraConfig collibraConfig;

    public CollibraValidatorService(CollibraConfig collibraConfig) {
        this.collibraConfig = collibraConfig;
    }

    @Override
    public Optional<FailedOperation> validate(DataProduct dataProduct) {
        logger.info("Validating data product, output port and column tags");
        // Tags in Collibra cannot have whitespace
        var dpTags =
                dataProduct.getTags().stream().filter(tag -> tag.getTagFQN().contains(" "));

        var opsTags = dataProduct.extractOutputPorts().stream().flatMap(outputPort -> {
            var opTags =
                    outputPort.getTags().stream().filter(tag -> tag.getTagFQN().contains(" "));
            var columnsTags = outputPort.getDataContract().getSchema().stream()
                    .flatMap(column -> column.getTags().stream()
                            .filter(tag -> tag.getTagFQN().contains(" ")));
            return Stream.concat(opTags, columnsTags);
        });

        var spacedTags = Stream.concat(dpTags, opsTags)
                .map(tag -> new Problem(String.format("Tag '%s' cannot have whitespace", tag.getTagFQN())))
                .toList();

        if (!spacedTags.isEmpty()) {
            logger.error("{} tags have whitespace on them: {}", spacedTags.size(), spacedTags);
            return Optional.of(new FailedOperation(spacedTags));
        }

        List<Problem> attributeMappingProblems = new ArrayList<>();
        logger.info("Validating data product, output port, and column attribute mapping");
        try {
            mapDataProductAttributes(collibraConfig, dataProduct);
        } catch (DataCatalogPluginValidationException ex) {
            logger.error("Error on validating data product attribute mapping", ex);
            attributeMappingProblems.addAll(ex.getFailedOperation().problems());
        }

        dataProduct.extractOutputPorts().forEach(outputPort -> {
            try {
                mapOutputPortAttributes(collibraConfig, outputPort);
            } catch (DataCatalogPluginValidationException ex) {
                logger.error(
                        String.format("Error on validating output port '%s' attribute mapping", outputPort.getId()),
                        ex);
                attributeMappingProblems.addAll(ex.getFailedOperation().problems());
            }
            outputPort.getDataContract().getSchema().forEach(column -> {
                try {
                    mapColumnAttributes(collibraConfig, column);
                } catch (DataCatalogPluginValidationException ex) {
                    logger.error(
                            String.format(
                                    "Error on validating column '%s' of output port '%s' attribute mapping",
                                    column.getName(), outputPort.getId()),
                            ex);
                    attributeMappingProblems.addAll(ex.getFailedOperation().problems());
                }
            });
        });

        if (!attributeMappingProblems.isEmpty()) {
            logger.error(
                    "{} attributes were not able to be mapped: {}",
                    attributeMappingProblems.size(),
                    attributeMappingProblems);
            return Optional.of(new FailedOperation(attributeMappingProblems));
        }

        logger.info("Data Product validation OK");
        return Optional.empty();
    }
}
