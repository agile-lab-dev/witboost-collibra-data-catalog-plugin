package it.agilelab.witboost.datacatalogplugin.collibra.controller;

import it.agilelab.witboost.datacatalogplugin.collibra.common.BusinessTermsPickerRetrieveException;
import it.agilelab.witboost.datacatalogplugin.collibra.common.BusinessTermsPickerValidationException;
import it.agilelab.witboost.datacatalogplugin.collibra.common.Problem;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.customurlpicker.CustomURLPickerError;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.customurlpicker.CustomURLPickerMalformedRequestError;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.customurlpicker.CustomURLPickerValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception handler for the Custom Url Picker API layer.
 *
 * <p>The following methods wrap generic exceptions into 400 and 500 errors. Implement your own
 * exception handlers based on the business exception that the provisioner throws. No further
 * modifications need to be done outside this file to make it work, as Spring identifies at startup
 * the handlers with the @ExceptionHandler annotation
 */
@RestControllerAdvice
public class CustomUrlPickerExceptionHandler {
    private final Logger logger = LoggerFactory.getLogger(CustomUrlPickerExceptionHandler.class);

    @ExceptionHandler({BusinessTermsPickerRetrieveException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected CustomURLPickerMalformedRequestError handleConflict(BusinessTermsPickerRetrieveException ex) {
        logger.error("Business Term picker retrieve Error", ex);
        return new CustomURLPickerMalformedRequestError(ex.getFailedOperation().problems().stream()
                .map(Problem::getMessage)
                .toList());
    }

    @ExceptionHandler({BusinessTermsPickerValidationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected CustomURLPickerValidationError handleConflict(BusinessTermsPickerValidationException ex) {
        logger.error("Business Term picker validation Error", ex);
        return new CustomURLPickerValidationError(ex.getFailedOperation().problems().stream()
                .map(p -> new CustomURLPickerError(p.getMessage()))
                .toList());
    }
}
