package it.agilelab.witboost.datacatalogplugin.collibra.controller;

import static org.mockito.Mockito.when;

import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.ProvisioningRequest;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.ValidationResult;
import it.agilelab.witboost.datacatalogplugin.collibra.service.DataCatalogService;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
public class DataCatalogPluginControllerTest {

    @Mock
    DataCatalogService datacatalogService;

    @InjectMocks
    DataCatalogPluginController dataCatalogPluginController;

    @Test
    void testValidate() throws Exception {
        ProvisioningRequest provisioningRequest = new ProvisioningRequest();

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockHttpServletRequest));

        when(datacatalogService.validate(provisioningRequest)).thenReturn(new ValidationResult(true));

        ResponseEntity<ValidationResult> responseEntity = dataCatalogPluginController.validate(provisioningRequest);
        Assertions.assertEquals(HttpStatusCode.valueOf(200), responseEntity.getStatusCode());
        Assertions.assertTrue(Objects.requireNonNull(responseEntity.getBody()).getValid());
    }
}
