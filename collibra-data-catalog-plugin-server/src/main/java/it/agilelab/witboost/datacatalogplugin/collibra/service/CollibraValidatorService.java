package it.agilelab.witboost.datacatalogplugin.collibra.service;

import it.agilelab.witboost.datacatalogplugin.collibra.common.FailedOperation;
import it.agilelab.witboost.datacatalogplugin.collibra.common.Problem;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.DataProduct;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CollibraValidatorService implements ValidatorService {

    private static final Logger logger = LoggerFactory.getLogger(CollibraValidatorService.class);

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
        logger.info("Data Product validation OK");
        return Optional.empty();
    }
}
