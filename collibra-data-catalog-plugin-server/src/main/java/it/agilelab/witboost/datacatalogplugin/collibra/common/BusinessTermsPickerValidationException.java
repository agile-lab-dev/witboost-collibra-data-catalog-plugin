package it.agilelab.witboost.datacatalogplugin.collibra.common;

public class BusinessTermsPickerValidationException extends RuntimeException {
    private final FailedOperation failedOperation;

    public BusinessTermsPickerValidationException(String message, FailedOperation failedOperation) {
        super(message);
        this.failedOperation = failedOperation;
    }

    public BusinessTermsPickerValidationException(String message, FailedOperation failedOperation, Throwable cause) {
        super(message, cause);
        this.failedOperation = failedOperation;
    }

    public FailedOperation getFailedOperation() {
        return failedOperation;
    }
}
