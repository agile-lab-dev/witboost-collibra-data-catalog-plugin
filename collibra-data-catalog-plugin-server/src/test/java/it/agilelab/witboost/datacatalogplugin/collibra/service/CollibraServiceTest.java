package it.agilelab.witboost.datacatalogplugin.collibra.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.vavr.control.Option;
import it.agilelab.witboost.datacatalogplugin.collibra.client.model.Asset;
import it.agilelab.witboost.datacatalogplugin.collibra.client.model.Community;
import it.agilelab.witboost.datacatalogplugin.collibra.client.model.Domain;
import it.agilelab.witboost.datacatalogplugin.collibra.common.*;
import it.agilelab.witboost.datacatalogplugin.collibra.config.CollibraAPIConfig;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.BusinessTerm;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.CustomUrlPickerRequest;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.DataProduct;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.DomainGroup;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.*;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.customurlpicker.CustomURLPickerItem;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.customurlpicker.CustomURLPickerResourcesRequestBody;
import it.agilelab.witboost.datacatalogplugin.collibra.service.client.CollibraApiClient;
import it.agilelab.witboost.datacatalogplugin.collibra.utils.ResourceUtils;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
class CollibraServiceTest {

    @Mock
    CollibraApiClient collibraApiClient;

    @Mock
    CollibraAPIConfig collibraAPIConfig;

    @Mock
    CollibraValidatorService collibraValidatorService;

    @InjectMocks
    CollibraService collibraService;

    @Test
    void validateOK() throws IOException {
        var yamlString = ResourceUtils.getContentFromResource("/descriptor.yaml");

        when(collibraValidatorService.validate(any(DataProduct.class))).thenReturn(Optional.empty());

        var expected = new ValidationResult(true);
        var actual = collibraService.validate(new ProvisioningRequest(DescriptorKind.DESCRIPTOR, yamlString));

        assertEquals(expected, actual);
    }

    @Test
    void validateError() throws IOException {
        var yamlString = ResourceUtils.getContentFromResource("/descriptor_malformed.yaml");

        var actual = collibraService.validate(new ProvisioningRequest(DescriptorKind.DESCRIPTOR, yamlString));

        assertFalse(actual.getValid());
        assertTrue(actual.getError().isPresent());
        actual.getError()
                .get()
                .getErrors()
                .forEach(p -> Assertions.assertTrue(p.startsWith("Failed to deserialize the Yaml Descriptor.")));
    }

    @Test
    void validateErrorOnValidator() throws IOException {
        var yamlString = ResourceUtils.getContentFromResource("/descriptor.yaml");

        var error = new FailedOperation(Collections.singletonList(new Problem("Error!")));

        when(collibraValidatorService.validate(any(DataProduct.class))).thenReturn(Optional.of(error));

        var actual = collibraService.validate(new ProvisioningRequest(DescriptorKind.DESCRIPTOR, yamlString));

        assertFalse(actual.getValid());
        assertTrue(actual.getError().isPresent());
        actual.getError().get().getErrors().forEach(p -> Assertions.assertEquals("Error!", p));
    }

    @Test
    void provisionOK() throws IOException {
        var yamlString = ResourceUtils.getContentFromResource("/descriptor.yaml");

        when(collibraValidatorService.validate(any(DataProduct.class))).thenReturn(Optional.empty());
        when(collibraApiClient.upsertDataProduct(any(DataProduct.class)))
                .thenReturn(new Asset().id(UUID.fromString("12345678-1234-1234-1234-1234567890ab")));
        when(collibraAPIConfig.endpoint()).thenReturn("https://my-collibra-instance.collibra.com");

        var expectedInfo = Map.of(
                "urn:dmb:cmp:finance:test-collibra:0:collibra-output-port",
                Map.of(
                        "collibra",
                        Map.of(
                                "type", "string",
                                "label", "Data Catalog",
                                "value", "View on Collibra",
                                "href",
                                        "https://my-collibra-instance.collibra.com/asset/12345678-1234-1234-1234-1234567890ab")));
        var actual = collibraService.provision(new ProvisioningRequest(DescriptorKind.DESCRIPTOR, yamlString));

        assertEquals(ProvisioningStatus.StatusEnum.COMPLETED, actual.getStatus());
        assertTrue(actual.getInfo().isPresent());
        assertEquals(expectedInfo, actual.getInfo().get().getPublicInfo());
    }

    @Test
    void provisionError() throws IOException {
        var yamlString = ResourceUtils.getContentFromResource("/descriptor.yaml");

        var error = new DataCatalogPluginProvisioningException(
                "Error while provisioning", new FailedOperation(Collections.singletonList(new Problem("Error!"))));

        when(collibraValidatorService.validate(any(DataProduct.class))).thenReturn(Optional.empty());
        when(collibraApiClient.upsertDataProduct(any(DataProduct.class))).thenThrow(error);

        var actual = assertThrows(
                DataCatalogPluginProvisioningException.class,
                () -> collibraService.provision(new ProvisioningRequest(DescriptorKind.DESCRIPTOR, yamlString)));
        assertEquals(
                "Error while provisioning data product on Collibra catalog. See error details for more information",
                actual.getMessage());
        assertEquals(actual.getCause(), error);
    }

    @Test
    void unprovisionOK() throws IOException {
        var yamlString = ResourceUtils.getContentFromResource("/descriptor.yaml");

        when(collibraValidatorService.validate(any(DataProduct.class))).thenReturn(Optional.empty());
        doNothing().when(collibraApiClient).deleteDataProduct(any(DataProduct.class));
        var actual = collibraService.unprovision(new ProvisioningRequest(DescriptorKind.DESCRIPTOR, yamlString));

        assertEquals(ProvisioningStatus.StatusEnum.COMPLETED, actual.getStatus());
    }

    @Test
    void unprovisionError() throws IOException {
        var yamlString = ResourceUtils.getContentFromResource("/descriptor.yaml");

        var error = new DataCatalogPluginProvisioningException(
                "Error while unprovisioning", new FailedOperation(Collections.singletonList(new Problem("Error!"))));

        when(collibraValidatorService.validate(any(DataProduct.class))).thenReturn(Optional.empty());
        doThrow(error).when(collibraApiClient).deleteDataProduct(any(DataProduct.class));
        var actual = assertThrows(
                DataCatalogPluginProvisioningException.class,
                () -> collibraService.unprovision(new ProvisioningRequest(DescriptorKind.DESCRIPTOR, yamlString)));
        assertEquals(
                "Error while unprovisioning data product on Collibra catalog. See error details for more information",
                actual.getMessage());
        assertEquals(actual.getCause(), error);
    }

    @Test
    void getBusinessTermsOKWithoutFilters() {
        var request =
                new CustomUrlPickerRequest(BigInteger.valueOf(0), BigInteger.valueOf(10), Option.none(), Option.none());
        var expected = List.of(
                new Asset()
                        .id(UUID.fromString("12345678-1234-1234-1234-1234567890ab"))
                        .displayName("my-bt"),
                new Asset()
                        .id(UUID.fromString("12345678-1234-1234-1234-1234567890ab"))
                        .displayName("my-bt-2"));
        when(collibraApiClient.findBusinessTermAssets(
                        BigInteger.valueOf(0), BigInteger.valueOf(10), Option.none(), Optional.empty()))
                .thenReturn(expected);

        var expectedReturn = List.of(
                new BusinessTerm("12345678-1234-1234-1234-1234567890ab", "my-bt"),
                new BusinessTerm("12345678-1234-1234-1234-1234567890ab", "my-bt-2"));

        var actual = collibraService.getBusinessTerms(request);

        assertEquals(expectedReturn, actual);
    }

    @Test
    void getBusinessTermsErrorWithoutFilters() {
        var request =
                new CustomUrlPickerRequest(BigInteger.valueOf(0), BigInteger.valueOf(10), Option.none(), Option.none());

        var error = new RestClientException("Error!");

        when(collibraApiClient.findBusinessTermAssets(
                        BigInteger.valueOf(0), BigInteger.valueOf(10), Option.none(), Optional.empty()))
                .thenThrow(error);

        var actual = assertThrows(
                BusinessTermsPickerRetrieveException.class, () -> collibraService.getBusinessTerms(request));
        assertEquals(
                "Error while retrieving Collibra business terms. See error details for more information",
                actual.getMessage());
        actual.getFailedOperation().problems().forEach(p -> {
            Assertions.assertTrue(p.description().startsWith("Error while retrieving Collibra business terms"));
            Assertions.assertTrue(p.cause().isPresent());
            Assertions.assertEquals(error, p.cause().get());
        });
    }

    @Test
    void getBusinessTermsOKWithDomainFilter() {
        var request = new CustomUrlPickerRequest(
                BigInteger.valueOf(0),
                BigInteger.valueOf(10),
                Option.none(),
                Option.some(new CustomURLPickerResourcesRequestBody().domain("domain:my-domain")));
        var expected = List.of(
                new Asset()
                        .id(UUID.fromString("12345678-1234-1234-1234-1234567890ab"))
                        .displayName("my-bt"),
                new Asset()
                        .id(UUID.fromString("12345678-1234-1234-1234-1234567890ab"))
                        .displayName("my-bt-2"));
        var domainGroup = new DomainGroup(
                new Community().name("my-domain"), new Domain().name("Data Products"), new Domain().name("Glossary"));
        when(collibraApiClient.findDomainByName("domain:my-domain")).thenReturn(Optional.of(domainGroup));
        when(collibraApiClient.findBusinessTermAssets(
                        BigInteger.valueOf(0), BigInteger.valueOf(10), Option.none(), Optional.of(domainGroup)))
                .thenReturn(expected);

        var expectedReturn = List.of(
                new BusinessTerm("12345678-1234-1234-1234-1234567890ab", "my-bt"),
                new BusinessTerm("12345678-1234-1234-1234-1234567890ab", "my-bt-2"));

        var actual = collibraService.getBusinessTerms(request);

        assertEquals(expectedReturn, actual);
    }

    @Test
    void getBusinessTermsErrorWithDomainFilterNotFound() {
        var request = new CustomUrlPickerRequest(
                BigInteger.valueOf(0),
                BigInteger.valueOf(10),
                Option.none(),
                Option.some(new CustomURLPickerResourcesRequestBody().domain("domain:my-domain")));
        when(collibraApiClient.findDomainByName("domain:my-domain")).thenReturn(Optional.empty());

        var actual = collibraService.getBusinessTerms(request);

        Assertions.assertTrue(actual.isEmpty());
    }

    @Test
    void getBusinessTermsErrorWithDomainFilterThrow() {
        var request = new CustomUrlPickerRequest(
                BigInteger.valueOf(0),
                BigInteger.valueOf(10),
                Option.none(),
                Option.some(new CustomURLPickerResourcesRequestBody().domain("domain:my-domain")));

        var error = new RestClientException("Error!");

        when(collibraApiClient.findDomainByName("domain:my-domain")).thenThrow(error);

        var actual = assertThrows(
                BusinessTermsPickerRetrieveException.class, () -> collibraService.getBusinessTerms(request));
        assertEquals(
                "Error while fetching Collibra domain 'domain:my-domain'. See error details for more information",
                actual.getMessage());
        actual.getFailedOperation().problems().forEach(p -> {
            Assertions.assertTrue(
                    p.description()
                            .startsWith(
                                    "Error while fetching Collibra domain 'domain:my-domain'. Collibra environment answered with an error"));
            Assertions.assertTrue(p.cause().isPresent());
            Assertions.assertEquals(error, p.cause().get());
        });
    }

    @Test
    void validateBusinessTerms() {
        var pickerItems = List.of(
                new CustomURLPickerItem("12345678-1234-1234-1234-1234567890ab", "my-bt"),
                new CustomURLPickerItem("12345678-1234-1234-1234-000000000000", "my-bt-2"));

        pickerItems.forEach(item -> when(collibraApiClient.findBusinessTermAsset(item.getId()))
                .thenReturn(
                        Option.of(new Asset().id(UUID.fromString(item.getId())).displayName(item.getValue()))));

        Assertions.assertDoesNotThrow(() -> collibraService.validateBusinessTerms(pickerItems, Option.none()));
    }

    @Test
    void validateBusinessTermsMissing() {
        var pickerItems = List.of(
                new CustomURLPickerItem("12345678-1234-1234-1234-1234567890ab", "my-bt"),
                new CustomURLPickerItem("12345678-1234-1234-1234-000000000000", "my-bt-2"));

        when(collibraApiClient.findBusinessTermAsset("12345678-1234-1234-1234-1234567890ab"))
                .thenReturn(Option.of(new Asset()
                        .id(UUID.fromString("12345678-1234-1234-1234-1234567890ab"))
                        .displayName("my-bt")));

        when(collibraApiClient.findBusinessTermAsset("12345678-1234-1234-1234-000000000000"))
                .thenReturn(Option.none());

        var actual = Assertions.assertThrows(
                BusinessTermsPickerValidationException.class,
                () -> collibraService.validateBusinessTerms(pickerItems, Option.none()));

        assertEquals(
                "1 business terms have an error when validating them against the Collibra environment. See error details for more information",
                actual.getMessage());
        assertEquals(1, actual.getFailedOperation().problems().size());
        actual.getFailedOperation().problems().forEach(p -> {
            Assertions.assertEquals(
                    "Couldn't find business term with id '12345678-1234-1234-1234-000000000000'", p.description());
            Assertions.assertTrue(p.cause().isEmpty());
        });
    }

    @Test
    void validateBusinessTermsMismatch() {
        var pickerItems = List.of(
                new CustomURLPickerItem("12345678-1234-1234-1234-1234567890ab", "my-bt"),
                new CustomURLPickerItem("12345678-1234-1234-1234-000000000000", "my-bt-2"));

        when(collibraApiClient.findBusinessTermAsset("12345678-1234-1234-1234-1234567890ab"))
                .thenReturn(Option.of(new Asset()
                        .id(UUID.fromString("12345678-1234-1234-1234-1234567890ab"))
                        .displayName("my-bt-wrong-value")));

        when(collibraApiClient.findBusinessTermAsset("12345678-1234-1234-1234-000000000000"))
                .thenReturn(Option.of(new Asset()
                        .id(UUID.fromString("12345678-1234-1234-1234-000000000000"))
                        .displayName("my-bt-2")));

        var actual = Assertions.assertThrows(
                BusinessTermsPickerValidationException.class,
                () -> collibraService.validateBusinessTerms(pickerItems, Option.none()));

        assertEquals(
                "1 business terms have an error when validating them against the Collibra environment. See error details for more information",
                actual.getMessage());
        assertEquals(1, actual.getFailedOperation().problems().size());
        actual.getFailedOperation().problems().forEach(p -> {
            Assertions.assertEquals(
                    "Content of business term with id '12345678-1234-1234-1234-1234567890ab' doesn't match with Collibra business term. Received: 'my-bt'. Actual in Collibra: 'my-bt-wrong-value'",
                    p.description());
            Assertions.assertTrue(p.cause().isEmpty());
        });
    }
}
