package it.agilelab.witboost.datacatalogplugin.collibra.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import it.agilelab.witboost.datacatalogplugin.collibra.common.CollibraAPIConfig;
import it.agilelab.witboost.datacatalogplugin.collibra.common.DataCatalogPluginValidationException;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.*;
import it.agilelab.witboost.datacatalogplugin.collibra.parser.Parser;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CollibraService implements DatacatalogService {

    private static final String STORAGE_KIND = "storage";
    private static final String OUTPUTPORT_KIND = "outputport";

    private final CollibraApiClient collibraApiClient;
    private final CollibraAPIConfig collibraAPIConfig;

    private final Logger logger = LoggerFactory.getLogger(CollibraService.class);

    public CollibraService(CollibraApiClient collibraApiClient, CollibraAPIConfig collibraAPIConfig) {
        this.collibraApiClient = collibraApiClient;
        this.collibraAPIConfig = collibraAPIConfig;
    }

    @Override
    public EntityReference getEntityReference(String componentId) {
        // TODO implement? this may be a leftover/unused endpoint
        return new EntityReference("{}");
    }

    @Override
    public ValidationResult validate(ProvisioningRequest provisioningRequest) {
        // TODO implement
        return new ValidationResult(true);
    }

    public ProvisioningStatus provision(ProvisioningRequest provisioningRequest) {
        // TODO validate

        var eitherDataProduct = Parser.parseDataProduct(provisioningRequest.getDescriptor());

        if (eitherDataProduct.isLeft()) {
            logger.error("Unable to parse descriptor; error: {}", eitherDataProduct.getLeft());
            throw new DataCatalogPluginValidationException(eitherDataProduct.getLeft());
        }

        var dataProduct = eitherDataProduct.get();

        var asset = collibraApiClient.upsertDataProduct(dataProduct);

        var outputPorts = collibraApiClient.extractOutputPorts(dataProduct);

        var collibraLink = Map.of(
                "collibra",
                Map.of(
                        "type", "string",
                        "label", "Data Catalog",
                        "value", "View on Collibra",
                        "href", collibraAPIConfig.basePath() + "/asset/" + asset.getId()));
        var publicInfo = new HashMap<String, Map<String, Map<String, String>>>();
        outputPorts.forEach(op -> publicInfo.put(op.getId(), collibraLink));

        var provisioningStatus = new ProvisioningStatus(
                        ProvisioningStatus.StatusEnum.COMPLETED, "Provisioned asset: " + asset)
                .info(new Info(publicInfo, JsonNodeFactory.instance.objectNode()));

        return provisioningStatus;
    }

    public ProvisioningStatus unprovision(ProvisioningRequest provisioningRequest) {
        // TODO implement
        return new ProvisioningStatus(ProvisioningStatus.StatusEnum.COMPLETED, "");
    }
}
