package it.agilelab.witboost.datacatalogplugin.collibra.common;

public class DataCatalogPluginProvisioningException extends RuntimeException {
    private final FailedOperation failedOperation;

    public DataCatalogPluginProvisioningException(String message, FailedOperation failedOperation) {
        super(message);
        this.failedOperation = failedOperation;
    }

    public FailedOperation getFailedOperation() {
        return failedOperation;
    }
}
