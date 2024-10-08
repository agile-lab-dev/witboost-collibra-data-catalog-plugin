package it.agilelab.witboost.datacatalogplugin.collibra.common;

public class BusinessTermsPickerRetrieveException extends RuntimeException {
    private final FailedOperation failedOperation;

    public BusinessTermsPickerRetrieveException(String message, FailedOperation failedOperation) {
        super(message);
        this.failedOperation = failedOperation;
    }

    public BusinessTermsPickerRetrieveException(String message, FailedOperation failedOperation, Throwable cause) {
        super(message, cause);
        this.failedOperation = failedOperation;
    }

    public FailedOperation getFailedOperation() {
        return failedOperation;
    }
}
