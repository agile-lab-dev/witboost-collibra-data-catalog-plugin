package it.agilelab.witboost.datacatalogplugin.collibra.controller;

import it.agilelab.witboost.datacatalogplugin.collibra.common.DataCatalogPluginProvisioningException;
import it.agilelab.witboost.datacatalogplugin.collibra.common.DataCatalogPluginValidationException;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.RequestValidationError;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.SystemError;
import java.util.ArrayList;
import java.util.List;
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
        List<String> list = new ArrayList<>();
        list.add(ex.getMessage());
        return new RequestValidationError(list);
    }

    @ExceptionHandler({DataCatalogPluginProvisioningException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected RequestValidationError handleConflict(DataCatalogPluginProvisioningException ex) {
        // TODO return provisioningstatus instead, missing errormoreinfo
        List<String> list = new ArrayList<>();
        list.add(ex.getMessage());
        return new RequestValidationError(list);
    }

    @ExceptionHandler({RuntimeException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    protected SystemError handleConflict(RuntimeException ex) {
        logger.error("Error", ex);
        return new SystemError(ex.getMessage());
    }
}
