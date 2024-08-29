package it.agilelab.witboost.datacatalogplugin.collibra.controller;

import it.agilelab.witboost.datacatalogplugin.collibra.common.*;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.RequestValidationError;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.SystemError;
import jakarta.validation.ConstraintViolationException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception handler for the API layer.
 *
 * <p>The following methods wrap generic exceptions into 400 and 500 errors. Implement your own
 * exception handlers based on the business exception that the provisioner throws. No further
 * modifications need to be done outside this file to make it work, as Spring identifies at startup
 * the handlers with the @ExceptionHandler annotation
 */
@RestControllerAdvice
public class DataCatalogPluginExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(DataCatalogPluginExceptionHandler.class);

    @ExceptionHandler({DataCatalogPluginValidationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected RequestValidationError handleConflict(DataCatalogPluginValidationException ex) {
        logger.error("DataCatalogPluginValidation Error", ex);
        return ErrorBuilder.buildRequestValidationError(Optional.ofNullable(ex.getMessage()), ex.getFailedOperation());
    }

    @ExceptionHandler({ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected RequestValidationError handleConflict(ConstraintViolationException ex) {
        logger.error("Constraint violation Error", ex);
        return ErrorBuilder.buildRequestValidationError(ex);
    }

    @ExceptionHandler({DataCatalogPluginProvisioningException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected RequestValidationError handleConflict(DataCatalogPluginProvisioningException ex) {
        logger.error("DataCatalogPluginProvisioning Error", ex);
        // TODO return provisioningstatus instead, missing errormoreinfo
        return ErrorBuilder.buildRequestValidationError(Optional.ofNullable(ex.getMessage()), ex.getFailedOperation());
    }

    @ExceptionHandler({RuntimeException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    protected SystemError handleConflict(RuntimeException ex) {
        logger.error("Error", ex);
        return ErrorBuilder.buildSystemError(Optional.empty(), ex);
    }
}
