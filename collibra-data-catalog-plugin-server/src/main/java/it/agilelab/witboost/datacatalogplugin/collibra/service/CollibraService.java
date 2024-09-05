package it.agilelab.witboost.datacatalogplugin.collibra.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import it.agilelab.witboost.datacatalogplugin.collibra.client.model.Asset;
import it.agilelab.witboost.datacatalogplugin.collibra.common.*;
import it.agilelab.witboost.datacatalogplugin.collibra.config.CollibraAPIConfig;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.BusinessTerm;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.CustomUrlPickerRequest;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.DataProduct;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.DomainGroup;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.*;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.customurlpicker.CustomURLPickerItem;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.customurlpicker.CustomURLPickerResourcesRequestBody;
import it.agilelab.witboost.datacatalogplugin.collibra.parser.Parser;
import it.agilelab.witboost.datacatalogplugin.collibra.service.client.CollibraApiClient;
import java.util.*;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CollibraService implements DataCatalogService, BusinessTermService {

    private final CollibraApiClient collibraApiClient;
    private final CollibraAPIConfig collibraAPIConfig;
    private final CollibraValidatorService collibraValidatorService;

    private final Logger logger = LoggerFactory.getLogger(CollibraService.class);

    public CollibraService(
            CollibraApiClient collibraApiClient,
            CollibraAPIConfig collibraAPIConfig,
            CollibraValidatorService collibraValidatorService) {
        this.collibraApiClient = collibraApiClient;
        this.collibraAPIConfig = collibraAPIConfig;
        this.collibraValidatorService = collibraValidatorService;
    }

    @Override
    public EntityReference getEntityReference(String componentId) {
        // TODO implement? this may be a leftover/unused endpoint
        return new EntityReference("{}");
    }

    @Override
    public ValidationResult validate(ProvisioningRequest provisioningRequest) {
        try {
            parseAndValidateDataProduct(provisioningRequest);
            return new ValidationResult(true);
        } catch (DataCatalogPluginValidationException ex) {
            return new ValidationResult(false)
                    .error(new ValidationError(ex.getFailedOperation().problems().stream()
                            .map(Problem::getMessage)
                            .toList()));
        }
    }

    public ProvisioningStatus provision(ProvisioningRequest provisioningRequest) {

        var dataProduct = parseAndValidateDataProduct(provisioningRequest);

        try {
            var asset = collibraApiClient.upsertDataProduct(dataProduct);

            var outputPorts = dataProduct.extractOutputPorts();

            var collibraLink = createDataProductPublicInfo(asset);
            var publicInfo = new HashMap<String, Map<String, Map<String, String>>>();
            outputPorts.forEach(op -> publicInfo.put(op.getId(), collibraLink));

            return new ProvisioningStatus(ProvisioningStatus.StatusEnum.COMPLETED, "Provisioned asset: " + asset)
                    .info(new Info(publicInfo, JsonNodeFactory.instance.objectNode()));
        } catch (Exception ex) {
            logger.error("Error while provisioning", ex);
            throw new DataCatalogPluginProvisioningException(
                    "Error while provisioning data product on Collibra catalog. See error details for more information",
                    new FailedOperation(Collections.singletonList(
                            new Problem("Error while provisioning data product on Collibra catalog", ex))),
                    ex);
        }
    }

    private DataProduct parseAndValidateDataProduct(ProvisioningRequest provisioningRequest) {
        logger.info("Parsing Data Product Descriptor");
        var eitherDataProduct = Parser.parseDataProduct(provisioningRequest.getDescriptor());

        if (eitherDataProduct.isLeft()) {
            logger.error("Unable to parse descriptor; error: {}", eitherDataProduct.getLeft());
            throw new DataCatalogPluginValidationException(
                    "Error while validating data product descriptor. See error details for more information",
                    eitherDataProduct.getLeft());
        }

        var dataProduct = eitherDataProduct.get();

        logger.info("Validating Data Product Descriptor");
        var validationResult = collibraValidatorService.validate(dataProduct);
        if (validationResult.isPresent()) {
            logger.error("Error while validating data product: {}", validationResult.get());
            throw new DataCatalogPluginValidationException(
                    "Error while validating data product descriptor. See error details for more information",
                    validationResult.get());
        } else return dataProduct;
    }

    private Map<String, Map<String, String>> createDataProductPublicInfo(Asset asset) {
        var trimmedBasePath = collibraAPIConfig.endpoint().endsWith("/")
                ? collibraAPIConfig
                        .endpoint()
                        .substring(0, collibraAPIConfig.endpoint().length() - 1)
                : collibraAPIConfig.endpoint();

        return Map.of(
                "collibra",
                Map.of(
                        "type", "string",
                        "label", "Data Catalog",
                        "value", "View on Collibra",
                        "href", trimmedBasePath + "/asset/" + asset.getId()));
    }

    public ProvisioningStatus unprovision(ProvisioningRequest provisioningRequest) {
        var dataProduct = parseAndValidateDataProduct(provisioningRequest);

        try {
            collibraApiClient.deleteDataProduct(dataProduct);
            return new ProvisioningStatus(ProvisioningStatus.StatusEnum.COMPLETED, "");
        } catch (Exception ex) {
            logger.error("Error while unprovisioning", ex);
            throw new DataCatalogPluginProvisioningException(
                    "Error while unprovisioning data product on Collibra catalog. See error details for more information",
                    new FailedOperation(Collections.singletonList(
                            new Problem("Error while unprovisioning data product on Collibra catalog", ex))),
                    ex);
        }
    }

    @Override
    public List<BusinessTerm> getBusinessTerms(CustomUrlPickerRequest request) {
        logger.info("Retrieving business terms from Collibra based on Request: {}", request);
        Optional<DomainGroup> domain = Optional.empty();
        Optional<String> optionalDomainRequest =
                request.queryParameters().toJavaOptional().flatMap(CustomURLPickerResourcesRequestBody::getDomain);
        if (optionalDomainRequest.isPresent()) {
            logger.info(
                    "Domain is present as part of request body. Querying Collibra for domain '{}'",
                    optionalDomainRequest.get());

            try {
                domain = collibraApiClient.findDomainByName(optionalDomainRequest.get());
            } catch (Exception ex) {
                throw new BusinessTermsPickerRetrieveException(
                        String.format(
                                "Error while fetching Collibra domain '%s'. See error details for more information",
                                optionalDomainRequest.get()),
                        new FailedOperation(Collections.singletonList(new Problem(
                                String.format(
                                        "Error while fetching Collibra domain '%s'. Collibra environment answered with an error",
                                        optionalDomainRequest.get()),
                                ex))),
                        ex);
            }

            logger.info(
                    "Domain request: {}, domain found: {}",
                    request.queryParameters().toJavaOptional().flatMap(CustomURLPickerResourcesRequestBody::getDomain),
                    domain.isPresent());
            logger.debug(
                    "Domain request: {}, domain found: {}",
                    request.queryParameters().toJavaOptional().flatMap(CustomURLPickerResourcesRequestBody::getDomain),
                    domain);

            if (domain.isEmpty()) {
                String errorMessage = String.format(
                        "Couldn't find domain with name '%s' on the configured Collibra environment",
                        optionalDomainRequest.get());
                logger.warn(errorMessage);
                return List.of();
            }
        }

        try {
            return collibraApiClient
                    .findBusinessTermAssets(request.offset(), request.limit(), request.filter(), domain)
                    .stream()
                    .map(BusinessTerm::new)
                    .toList();
        } catch (Exception ex) {
            throw new BusinessTermsPickerRetrieveException(
                    "Error while retrieving Collibra business terms. See error details for more information",
                    new FailedOperation(Collections.singletonList(
                            new Problem("Error while retrieving Collibra business terms", ex))),
                    ex);
        }
    }

    @Override
    public void validateBusinessTerms(List<CustomURLPickerItem> businessTerms, Option<CustomUrlPickerRequest> request) {
        logger.info("Validating {} business terms", businessTerms.size());
        logger.debug(
                "Validating {} business terms: {} with request parameters {}",
                businessTerms.size(),
                businessTerms,
                request);

        List<Tuple2<BusinessTerm, Option<BusinessTerm>>> results;
        var collibraBusinessTerms = businessTerms.stream()
                .map(businessTerm -> new BusinessTerm(businessTerm.getId(), businessTerm.getValue()))
                .toList();

        try {
            logger.info(
                    "Retrieving assets with ids: {}",
                    collibraBusinessTerms.stream().map(BusinessTerm::id).toList());
            results = collibraBusinessTerms.stream()
                    .map(businessTerm -> new Tuple2<>(
                            businessTerm,
                            collibraApiClient
                                    .findBusinessTermAsset(businessTerm.id())
                                    .map(BusinessTerm::new)))
                    .toList();
        } catch (Exception ex) {
            logger.error("Error while retrieving assets from Collibra client", ex);
            throw new BusinessTermsPickerValidationException(
                    "Error while validating Collibra business terms. See error details for more information",
                    new FailedOperation(Collections.singletonList(
                            new Problem("Error while validating Collibra business terms", ex))),
                    ex);
        }

        var assetsNotFound = results.stream().filter(t -> t._2.isEmpty()).toList();

        var assetsMismatch = results.stream()
                .filter(t -> t._2.fold(() -> false, asset -> !t._1.equals(asset)))
                .toList();

        logger.info(
                "From {} business terms to be validated, {} are OK, {} are not found, and {} mismatch",
                businessTerms.size(),
                (businessTerms.size() - assetsNotFound.size() - assetsMismatch.size()),
                assetsNotFound.size(),
                assetsMismatch.size());
        logger.debug(
                "From {} business terms to be validated, {} are not found, and {} mismatch",
                businessTerms.stream().map(CustomURLPickerItem::getId).toList(),
                assetsNotFound.stream().map(t -> t._1.id()).toList(),
                assetsMismatch.stream()
                        .map(t -> String.format(
                                "Received: %s. Actual in Collibra: %s",
                                t._1.value(), t._2.get().value()))
                        .toList());

        var errors = Stream.concat(
                        assetsNotFound.stream()
                                .map(t -> new Problem(
                                        String.format("Couldn't find business term with id '%s'", t._1.id()))),
                        assetsMismatch.stream()
                                .map(t -> new Problem(String.format(
                                        "Content of business term with id '%s' doesn't match with Collibra business term. Received: '%s'. Actual in Collibra: '%s'",
                                        t._1.id(), t._1.value(), t._2.get().value()))))
                .toList();
        if (!errors.isEmpty()) {
            logger.error("Errors found while retrieving business terms from Collibra environment: {}", errors);
            throw new BusinessTermsPickerValidationException(
                    String.format(
                            "%s business terms have an error when validating them against the Collibra environment. See error details for more information",
                            errors.size()),
                    new FailedOperation(errors));
        }
    }
}
