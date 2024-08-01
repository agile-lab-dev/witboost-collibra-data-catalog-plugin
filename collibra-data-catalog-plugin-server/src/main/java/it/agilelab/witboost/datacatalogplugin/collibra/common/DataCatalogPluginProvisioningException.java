package it.agilelab.witboost.datacatalogplugin.collibra.common;

public class DataCatalogPluginProvisioningException extends RuntimeException {
    private final FailedOperation failedOperation;

    public DataCatalogPluginProvisioningException(FailedOperation failedOperation) {
        super();
        this.failedOperation = failedOperation;
    }

    public FailedOperation getFailedOperation() {
        return failedOperation;
    }
}
