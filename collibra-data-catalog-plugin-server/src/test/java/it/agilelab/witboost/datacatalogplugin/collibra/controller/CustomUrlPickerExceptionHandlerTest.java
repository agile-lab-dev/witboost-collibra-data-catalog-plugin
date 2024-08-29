package it.agilelab.witboost.datacatalogplugin.collibra.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.agilelab.witboost.datacatalogplugin.collibra.common.*;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.customurlpicker.CustomURLPickerError;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(CustomUrlPickerExceptionHandler.class)
class CustomUrlPickerExceptionHandlerTest {
    @InjectMocks
    CustomUrlPickerExceptionHandler customUrlPickerExceptionHandler;

    @Test
    void testHandleConflictRequestValidationError() {
        FailedOperation failedOperation = new FailedOperation(List.of(
                new Problem("Error1 - No cause"),
                new Problem("Error2 - cause", new Exception("Cause message")),
                new Problem("Error3 - solutions", Optional.empty(), Set.of("Try again"))));

        var error = customUrlPickerExceptionHandler.handleConflict(
                new BusinessTermsPickerValidationException("Error! See info", failedOperation));
        var errorRetrieve = customUrlPickerExceptionHandler.handleConflict(
                new BusinessTermsPickerRetrieveException("Error! See info", failedOperation));

        var apiErrors = List.of(
                new CustomURLPickerError("Error1 - No cause"),
                new CustomURLPickerError("Error2 - cause: Cause message"),
                new CustomURLPickerError("Error3 - solutions"));

        assertEquals(error.getErrors(), apiErrors);
        assertEquals(
                errorRetrieve.getErrors(),
                apiErrors.stream().map(CustomURLPickerError::getError).toList());
    }
}
