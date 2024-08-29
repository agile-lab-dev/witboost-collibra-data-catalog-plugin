package it.agilelab.witboost.datacatalogplugin.collibra.controller;

import io.vavr.control.Option;
import it.agilelab.witboost.datacatalogplugin.collibra.common.BusinessTermsPickerRetrieveException;
import it.agilelab.witboost.datacatalogplugin.collibra.common.FailedOperation;
import it.agilelab.witboost.datacatalogplugin.collibra.common.Problem;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.CustomUrlPickerRequest;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.controller.V1CustomURLPickerApiDelegate;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.customurlpicker.CustomURLPickerItem;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.customurlpicker.CustomURLPickerResourcesRequestBody;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.customurlpicker.CustomURLPickerValidationRequest;
import it.agilelab.witboost.datacatalogplugin.collibra.service.BusinessTermService;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * API Controller for the Custom URL Picker endpoints which implements the autogenerated {@link
 * V1CustomURLPickerApiDelegate} interface. The interface defaults the endpoints to throw
 * 501 Not Implemented unless overridden in this class.
 *
 * <p>Exceptions thrown will be handled by {@link DataCatalogPluginExceptionHandler}
 */
@Service
public class CustomUrlPickerController implements V1CustomURLPickerApiDelegate {

    private final BusinessTermService businessTermService;

    public CustomUrlPickerController(BusinessTermService businessTermService) {
        this.businessTermService = businessTermService;
    }

    @Override
    public ResponseEntity<List<CustomURLPickerItem>> retrieveValues(
            BigDecimal offset,
            BigDecimal limit,
            Optional<String> filter,
            Optional<CustomURLPickerResourcesRequestBody> body)
            throws Exception {
        return ResponseEntity.ok(businessTermService
                .getBusinessTerms(new CustomUrlPickerRequest(
                        offset.toBigInteger(),
                        limit.toBigInteger(),
                        Option.ofOptional(filter),
                        Option.ofOptional(body)))
                .stream()
                .map(businessTerm -> new CustomURLPickerItem(businessTerm.id(), businessTerm.value()))
                .toList());
    }

    @Override
    public ResponseEntity<String> validate(Optional<CustomURLPickerValidationRequest> customURLPickerValidationRequest)
            throws Exception {
        if (customURLPickerValidationRequest.isEmpty())
            throw new BusinessTermsPickerRetrieveException(
                    "Couldn't validate request, received empty body",
                    new FailedOperation(Collections.singletonList(new Problem("Received empty body"))));

        var queryParams = Option.ofOptional(
                        customURLPickerValidationRequest.flatMap(CustomURLPickerValidationRequest::getQueryParameters))
                .map(CustomUrlPickerRequest::new);
        businessTermService.validateBusinessTerms(
                customURLPickerValidationRequest.get().getSelectedObjects(), queryParams);

        return ResponseEntity.ok("OK");
    }
}