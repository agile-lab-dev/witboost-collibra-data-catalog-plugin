package it.agilelab.witboost.datacatalogplugin.collibra.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.vavr.control.Option;
import it.agilelab.witboost.datacatalogplugin.collibra.common.BusinessTermsPickerRetrieveException;
import it.agilelab.witboost.datacatalogplugin.collibra.common.BusinessTermsPickerValidationException;
import it.agilelab.witboost.datacatalogplugin.collibra.common.FailedOperation;
import it.agilelab.witboost.datacatalogplugin.collibra.common.Problem;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.BusinessTerm;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.CustomUrlPickerRequest;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.customurlpicker.CustomURLPickerItem;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.customurlpicker.CustomURLPickerValidationRequest;
import it.agilelab.witboost.datacatalogplugin.collibra.service.BusinessTermService;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;

@ExtendWith(MockitoExtension.class)
class CustomUrlPickerControllerTest {

    @Mock
    BusinessTermService businessTermService;

    @InjectMocks
    CustomUrlPickerController controller;

    @Test
    void retrieveValues() throws Exception {

        var expected = List.of(new CustomURLPickerItem("id-1", "value-1"), new CustomURLPickerItem("id-2", "value-2"));

        when(businessTermService.getBusinessTerms(new CustomUrlPickerRequest(
                        BigInteger.valueOf(0), BigInteger.valueOf(10), Option.none(), Option.none())))
                .thenReturn(List.of(new BusinessTerm("id-1", "value-1"), new BusinessTerm("id-2", "value-2")));

        var actual = controller.retrieveValues(
                BigDecimal.valueOf(0), BigDecimal.valueOf(10), Optional.empty(), Optional.empty());

        Assertions.assertEquals(HttpStatusCode.valueOf(200), actual.getStatusCode());
        Assertions.assertEquals(expected, Objects.requireNonNull(actual.getBody()));
    }

    @Test
    void retrieveValuesError() throws Exception {

        var error = new BusinessTermsPickerRetrieveException(
                "Error!", new FailedOperation(Collections.singletonList(new Problem("Error!"))));

        when(businessTermService.getBusinessTerms(new CustomUrlPickerRequest(
                        BigInteger.valueOf(0), BigInteger.valueOf(10), Option.none(), Option.none())))
                .thenThrow(error);

        Assertions.assertThrows(
                BusinessTermsPickerRetrieveException.class,
                () -> controller.retrieveValues(
                        BigDecimal.valueOf(0), BigDecimal.valueOf(10), Optional.empty(), Optional.empty()));
    }

    @Test
    void validate() throws Exception {
        var objects = List.of(new CustomURLPickerItem("id", "value"));
        var request = new CustomURLPickerValidationRequest(objects);

        doNothing().when(businessTermService).validateBusinessTerms(objects, Option.none());

        var actual = controller.validate(Optional.of(request));

        Assertions.assertEquals(HttpStatusCode.valueOf(200), actual.getStatusCode());
    }

    @Test
    void validateError() throws Exception {
        var objects = List.of(new CustomURLPickerItem("id", "value"));
        var request = new CustomURLPickerValidationRequest(objects);

        var error = new BusinessTermsPickerValidationException(
                "Error on validation",
                new FailedOperation(Collections.singletonList(new Problem("Error on validation"))));

        doThrow(error).when(businessTermService).validateBusinessTerms(objects, Option.none());

        Assertions.assertThrows(
                BusinessTermsPickerValidationException.class, () -> controller.validate(Optional.of(request)));
    }
}
