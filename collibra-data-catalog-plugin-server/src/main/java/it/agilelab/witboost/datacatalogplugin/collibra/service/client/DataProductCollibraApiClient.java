package it.agilelab.witboost.datacatalogplugin.collibra.service.client;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import it.agilelab.witboost.datacatalogplugin.collibra.client.api.AssetsApi;
import it.agilelab.witboost.datacatalogplugin.collibra.client.api.AttributesApi;
import it.agilelab.witboost.datacatalogplugin.collibra.client.api.RelationsApi;
import it.agilelab.witboost.datacatalogplugin.collibra.client.model.*;
import it.agilelab.witboost.datacatalogplugin.collibra.config.CollibraConfig;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.*;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.Tag;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataProductCollibraApiClient {

    private final CollibraConfig collibraConfig;
    private final AssetsApi assetsApiClient;
    private final AttributesApi attributesApiClient;
    private final RelationsApi relationsApiClient;
    private final DomainCollibraApiClient domainCollibraApiClient;
    private final Logger logger = LoggerFactory.getLogger(DataProductCollibraApiClient.class);

    public DataProductCollibraApiClient(
            CollibraConfig collibraConfig,
            AssetsApi assetsApiClient,
            AttributesApi attributesApiClient,
            RelationsApi relationsApiClient,
            DomainCollibraApiClient domainCollibraApiClient) {
        this.collibraConfig = collibraConfig;
        this.assetsApiClient = assetsApiClient;
        this.attributesApiClient = attributesApiClient;
        this.relationsApiClient = relationsApiClient;
        this.domainCollibraApiClient = domainCollibraApiClient;
    }

    public Asset createDataProduct(DataProduct dataProduct, UUID domainId) {
        logger.info(
                "Creating Data Product '{}' on Collibra environment under domain '{}'", dataProduct.getId(), domainId);
        var addAssetRequest = new AddAssetRequest();

        addAssetRequest
                .name(dataProduct.getId())
                .displayName(dataProduct.getName())
                .statusId(UUID.fromString(collibraConfig.initialStatusId()))
                .domainId(domainId)
                .typeId(UUID.fromString(collibraConfig.assets().dataProduct().typeId()))
                .excludedFromAutoHyperlinking(true);

        var asset = assetsApiClient.addAsset(addAssetRequest);
        logger.info("Data Product '{}' under domain '{}' created successfully", dataProduct.getId(), domainId);

        updateDataProductAttributes(asset.getId(), dataProduct);

        var outputPorts = dataProduct.extractOutputPorts();
        updateOutputPorts(domainId, asset.getId(), outputPorts);

        return asset;
    }

    public Asset updateDataProduct(UUID id, DataProduct dataProduct, UUID domainId) {
        var changeAssetRequest = new ChangeAssetRequest();

        changeAssetRequest
                .name(dataProduct.getId())
                .displayName(dataProduct.getName())
                .statusId(UUID.fromString(collibraConfig.initialStatusId()))
                .domainId(domainId)
                .typeId(UUID.fromString(collibraConfig.assets().dataProduct().typeId()))
                .excludedFromAutoHyperlinking(true);

        var asset = assetsApiClient.changeAsset(id, changeAssetRequest);

        updateDataProductAttributes(asset.getId(), dataProduct);

        var outputPorts = dataProduct.extractOutputPorts();
        updateOutputPorts(domainId, asset.getId(), outputPorts);

        return asset;
    }

    private void updateDataProductAttributes(UUID id, DataProduct dataProduct) {
        logger.info("Updating attributes and tags on Data Product Asset '{}'", id);

        var tags = dataProduct.getTags().stream().map(Tag::getTagFQN).toList();
        if (!tags.isEmpty()) {
            logger.debug("Setting tags '{}' to Asset '{}'", tags, id);
            var tagsRequest = new SetAssetTagsRequest().tagNames(tags);
            assetsApiClient.setTagsForAsset(id, tagsRequest);
            logger.info("Tags added succesfully to Asset '{}'", id);
        }
        logger.debug("Cleaning up existing attributes to override with new ones");
        var attributes = findAttributesForAsset(id);
        logger.debug("Removing attributes {}", attributes);
        var attributeIds = attributes.stream().map(Attribute::getId).collect(Collectors.toList());
        attributesApiClient.removeAttributes(attributeIds);

        var descriptionAttributeTypeId = UUID.fromString(
                collibraConfig.assets().dataProduct().attributes().description());
        List<Tuple2<UUID, String>> newAttributes =
                List.of(Tuple.of(descriptionAttributeTypeId, dataProduct.getDescription()));

        logger.debug("Adding attributes {} to Asset '{}'", newAttributes, id);
        newAttributes.stream()
                .map(newAttribute -> new AddAttributeRequest()
                        .assetId(id)
                        .typeId(newAttribute._1)
                        .value(newAttribute._2))
                .forEach(attributesApiClient::addAttribute);
        logger.info("Attributes added successfully to Asset '{}'", id);
    }

    private List<Attribute> findAttributesForAsset(UUID id) {
        logger.info("Querying for attributes of Asset '{}'", id);
        var attributes = attributesApiClient.findAttributes(0, 1000, null, id, null, null);
        return attributes.getResults();
    }

    private void removeOutputPorts(UUID dataProductId) {
        logger.info("Removing all Output Ports of Data Product Asset '{}'", dataProductId);
        var dataProductRelations = relationsApiClient
                .findRelations(
                        0,
                        1000,
                        UUID.fromString(collibraConfig.assets().dataProduct().containsOutputPortRelationId()),
                        dataProductId,
                        null,
                        null)
                .getResults();

        logger.debug(
                "Data Product has the following Output Ports: {}",
                dataProductRelations.stream().map(r -> r.getTarget().getId()).toList());
        for (Relation dataProductRelation : dataProductRelations) {
            var targetOutputPortId = dataProductRelation.getTarget().getId();

            logger.debug("Querying for Output Port '{}' relations", targetOutputPortId);
            var outputPortRelations = relationsApiClient
                    .findRelations(0, 1000, null, targetOutputPortId, null, null)
                    .getResults();

            logger.debug(
                    "Output Port has the following columns: {}",
                    outputPortRelations.stream().map(r -> r.getTarget().getId()).toList());
            for (Relation outputPortRelation : outputPortRelations) {
                var targetColumnId = outputPortRelation.getTarget().getId();
                logger.debug(
                        "Removing relation between Output Port '{}' and Column '{}'",
                        targetOutputPortId,
                        targetColumnId);
                relationsApiClient.removeRelation1(outputPortRelation.getId());
                logger.debug("Removing Column '{}'", targetColumnId);
                assetsApiClient.removeAsset(targetColumnId);
                logger.info("Column '{}' deleted successfully", targetColumnId);

                // TODO If we add other relations to column, we have to destroy those as well
            }

            logger.debug(
                    "Removing relation between Data Product '{}' and Output Port '{}'",
                    dataProductId,
                    targetOutputPortId);
            relationsApiClient.removeRelation1(dataProductRelation.getId());
            logger.debug("Removing Output Port '{}'", targetOutputPortId);
            assetsApiClient.removeAsset(targetOutputPortId);
            logger.info("Output Port '{}' deleted successfully", targetOutputPortId);
        }
    }

    private void updateOutputPorts(UUID domainId, UUID dataProductId, List<OutputPort<Specific>> outputPorts) {
        logger.info("Updating Output Ports for Data Product Asset '{}'", dataProductId);

        // TODO Should we be cleaning like this? What if users are depending on a specific id for the table they're
        //  checking out?
        logger.info("Executing Output Port cleanup before update");
        removeOutputPorts(dataProductId);
        logger.info("Output Port cleanup before update completed");

        if (outputPorts.isEmpty()) logger.warn("Data Product has no valid Output Ports");
        outputPorts.forEach(outputPort -> createOutputPort(domainId, dataProductId, outputPort));
    }

    private void createOutputPort(UUID domainId, UUID dataProductId, OutputPort<Specific> outputPort) {
        logger.info("Adding Output Port: '{}'", outputPort.getId());
        logger.debug("Adding Output Port with body: {}", outputPort);

        var addOutputPortRequest = new AddAssetRequest()
                .domainId(domainId)
                .name(outputPort.getId())
                .displayName(outputPort.getName())
                .typeId(UUID.fromString(collibraConfig.assets().outputPort().typeId()))
                .excludedFromAutoHyperlinking(true);

        var outputPortAsset = assetsApiClient.addAsset(addOutputPortRequest);
        logger.info("Output Port asset added with ID '{}'", outputPortAsset.getId());
        logger.debug("Output Port asset added with body {}", outputPortAsset);

        logger.info("Adding attributes to Output Port Asset with ID '{}'", outputPortAsset.getId());
        updateOutputPortAttributes(outputPortAsset.getId(), outputPort);
        logger.info("Output Port Asset '{}' attributes added successfully", outputPortAsset.getId());

        var addDataProductRelationRequest = new AddRelationRequest();
        addDataProductRelationRequest
                .sourceId(dataProductId)
                .targetId(outputPortAsset.getId())
                .typeId(UUID.fromString(collibraConfig.assets().dataProduct().containsOutputPortRelationId()));

        logger.info("Adding relation DataProduct '{}' > Output Port '{}'", dataProductId, outputPortAsset.getId());
        logger.debug("Adding relation DataProduct > Output Port with body {}", addDataProductRelationRequest);
        var relation = relationsApiClient.addRelation(addDataProductRelationRequest);
        logger.info("Relation DataProduct > Output Port added with ID '{}'", relation.getId());
        logger.debug("Relation DataProduct > Output Port added with body {}", relation);

        var columns = outputPort.getDataContract().getSchema();

        for (Column column : columns) {
            var addAssetRequest = new AddAssetRequest();

            addAssetRequest
                    .domainId(domainId)
                    .name(outputPort.getId() + ":" + column.getName())
                    .displayName(outputPort.getName() + " > " + column.getName())
                    .typeId(UUID.fromString(collibraConfig.assets().column().typeId()))
                    .excludedFromAutoHyperlinking(true);

            logger.info("Adding column '{}' asset", addAssetRequest.getDisplayName());
            logger.debug("Adding column asset {}", addAssetRequest);
            var columnAsset = assetsApiClient.addAsset(addAssetRequest);
            logger.info("Column asset added with ID '{}'", columnAsset.getId());
            logger.debug("Column asset added with body {}", columnAsset);

            logger.info("Adding attributes to column Asset with ID '{}'", columnAsset.getId());
            updateColumnAttributes(columnAsset.getId(), column);
            logger.info("Column Asset '{}' attributes added successfully", columnAsset.getId());

            var addOutputPortRelationRequest = new AddRelationRequest();
            addOutputPortRelationRequest
                    .sourceId(outputPortAsset.getId())
                    .targetId(columnAsset.getId())
                    .typeId(UUID.fromString(collibraConfig.assets().outputPort().containsColumnRelationId()));

            logger.info("Adding relation Output Port {} > column {}", outputPortAsset.getId(), columnAsset.getId());
            logger.debug("Adding relation Output Port > column with body {}", addOutputPortRelationRequest);
            var outputPortRelation = relationsApiClient.addRelation(addOutputPortRelationRequest);
            logger.info("Relation Output Port > column added with ID '{}'", outputPortRelation.getId());
            logger.debug("Relation Output Port > column added with body {}", outputPortRelation);
        }
    }

    private void updateOutputPortAttributes(UUID id, OutputPort<Specific> outputPort) {
        logger.info("Updating attributes and tags on Output Port Asset '{}'", id);

        var tags = outputPort.getTags().stream().map(Tag::getTagFQN).toList();
        if (!tags.isEmpty()) {
            logger.debug("Setting tags '{}' to Asset '{}'", tags, id);
            var tagsRequest = new SetAssetTagsRequest().tagNames(tags);
            assetsApiClient.setTagsForAsset(id, tagsRequest);
            logger.info("Tags added succesfully to Asset '{}'", id);
        }
        logger.debug("Cleaning up existing attributes to override with new ones");
        var attributes = findAttributesForAsset(id);
        logger.debug("Removing attributes {}", attributes);
        var attributeIds = attributes.stream().map(Attribute::getId).collect(Collectors.toList());
        attributesApiClient.removeAttributes(attributeIds);

        var descriptionAttributeTypeId = UUID.fromString(
                collibraConfig.assets().outputPort().attributes().description());
        var tableTypeAttributeTypeId = UUID.fromString(
                collibraConfig.assets().outputPort().attributes().tableType());
        List<Tuple2<UUID, String>> newAttributes = List.of(
                Tuple.of(descriptionAttributeTypeId, outputPort.getDescription()),
                Tuple.of(tableTypeAttributeTypeId, outputPort.getOutputPortType()));
        logger.debug("Adding attributes {} to Asset '{}'", newAttributes, id);

        newAttributes.stream()
                .map(newAttribute -> new AddAttributeRequest()
                        .assetId(id)
                        .typeId(newAttribute._1)
                        .value(newAttribute._2))
                .forEach(attributesApiClient::addAttribute);
        logger.info("Attributes added successfully to Asset '{}'", id);
    }

    private void updateColumnAttributes(UUID id, Column column) {
        logger.info("Updating attributes and tags on Data Product Asset '{}'", id);

        var tags = column.getTags().stream().map(Tag::getTagFQN).toList();
        if (!tags.isEmpty()) {
            logger.debug("Setting tags '{}' to Asset '{}'", tags, id);
            var tagsRequest = new SetAssetTagsRequest().tagNames(tags);
            assetsApiClient.setTagsForAsset(id, tagsRequest);
            logger.info("Tags added succesfully to Asset '{}'", id);
        }
        logger.debug("Cleaning up existing attributes to override with new ones");
        var attributes = findAttributesForAsset(id);
        logger.debug("Removing attributes {}", attributes);
        var attributeIds = attributes.stream().map(Attribute::getId).collect(Collectors.toList());
        attributesApiClient.removeAttributes(attributeIds);

        var descriptionAttributeTypeId =
                UUID.fromString(collibraConfig.assets().column().attributes().description());
        var tableTypeAttributeTypeId =
                UUID.fromString(collibraConfig.assets().column().attributes().dataType());
        List<Tuple2<UUID, String>> newAttributes = List.of(
                Tuple.of(descriptionAttributeTypeId, column.getDescription()),
                Tuple.of(tableTypeAttributeTypeId, column.getDataType()));

        logger.debug("Adding attributes {} to Asset '{}'", newAttributes, id);
        newAttributes.stream()
                .map(newAttribute -> new AddAttributeRequest()
                        .assetId(id)
                        .typeId(newAttribute._1)
                        .value(newAttribute._2))
                .forEach(attributesApiClient::addAttribute);
        logger.info("Attributes added successfully to Asset '{}'", id);
    }

    public Optional<Asset> findAssetForDataProduct(DataProduct dataProduct) {
        logger.info("Querying for Data Product '{}' on Collibra", dataProduct.getId());
        var response = assetsApiClient.findAssets(
                0, 10, dataProduct.getId(), "EXACT", null, null, null, null, null, null, null, null, null);

        return response.getResults().stream().findFirst();
    }

    public Asset upsertDataProduct(DataProduct dataProduct) {
        logger.info("Upserting Witboost Data Product '{}'", dataProduct.getId());

        var domain = domainCollibraApiClient.upsertDomain(dataProduct.getDomain());

        var maybeAsset = findAssetForDataProduct(dataProduct);

        if (maybeAsset.isEmpty()) {
            logger.info(
                    "Data Product '{}' is not present on Collibra environment or is incomplete, creating...",
                    dataProduct.getId());
            return createDataProduct(dataProduct, domain.dataAssetDomain().getId());
        } else {
            var id = maybeAsset.get().getId();
            logger.info(
                    "Data Product '{}' is present on Collibra environment with ID '{}', updating...",
                    dataProduct.getId(),
                    id);

            return updateDataProduct(id, dataProduct, domain.dataAssetDomain().getId());
        }
    }
}
