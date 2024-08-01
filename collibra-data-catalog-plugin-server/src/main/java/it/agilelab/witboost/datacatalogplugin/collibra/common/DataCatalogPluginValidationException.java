package it.agilelab.witboost.datacatalogplugin.collibra.common;

public class DataCatalogPluginValidationException extends RuntimeException {
    private final FailedOperation failedOperation;

    public DataCatalogPluginValidationException(FailedOperation failedOperation) {
        super();
        this.failedOperation = failedOperation;
    }

    public FailedOperation getFailedOperation() {
        return failedOperation;
    }
}
