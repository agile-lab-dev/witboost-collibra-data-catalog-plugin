package it.agilelab.witboost.datacatalogplugin.collibra.controller;

import static org.junit.jupiter.api.Assertions.*;

import it.agilelab.witboost.datacatalogplugin.collibra.common.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(DataCatalogPluginExceptionHandler.class)
class DataCatalogPluginExceptionHandlerTest {
    @InjectMocks
    DataCatalogPluginExceptionHandler dataCatalogPluginExceptionHandler;

    @Test
    void testHandleConflictRequestValidationError() {
        FailedOperation failedOperation = new FailedOperation(List.of(
                new Problem("Error1 - No cause"),
                new Problem("Error2 - cause", new Exception("Cause message")),
                new Problem("Error3 - solutions", Optional.empty(), Set.of("Try again"))));

        var customMessage = "Error! See info";

        var error = dataCatalogPluginExceptionHandler.handleConflict(
                new DataCatalogPluginValidationException(customMessage, failedOperation));

        assertEquals(error.getUserMessage(), Optional.of(customMessage));
        assertTrue(error.getMoreInfo().isPresent());
        assertTrue(error.getInputErrorField().isEmpty());
        assertTrue(error.getInput().isEmpty());
        assertEquals(error.getErrors(), error.getMoreInfo().get().getProblems());
        assertEquals(error.getMoreInfo().get().getProblems().size(), 3);
        assertEquals(error.getMoreInfo().get().getSolutions().size(), 2);
        assertTrue(error.getMoreInfo().get().getSolutions().contains("Try again"));
        assertEquals(
                error.getMoreInfo().get().getProblems(),
                List.of("Error1 - No cause", "Error2 - cause: Cause message", "Error3 - solutions"));
    }

    @Test
    void testHandleConflict() {
        var exception = new RuntimeException("System error!");

        var actual = dataCatalogPluginExceptionHandler.handleConflict(exception);

        assertEquals(
                Optional.of(
                        "An unexpected error occurred while processing the request. Check the error details for more information"),
                actual.getUserMessage());
        assertTrue(actual.getMoreInfo().isPresent());
        assertTrue(actual.getMoreInfo().get().getProblems().contains(actual.getError()));
        assertTrue(actual.getMoreInfo().get().getProblems().contains(exception.getMessage()));
        assertEquals(
                actual.getMoreInfo().get().getSolutions(),
                List.of("Please try again and if the problem persists contact the platform team."));
    }
}
