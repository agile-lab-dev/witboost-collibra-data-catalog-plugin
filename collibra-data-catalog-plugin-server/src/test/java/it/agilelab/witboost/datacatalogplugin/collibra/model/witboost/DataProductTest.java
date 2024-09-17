package it.agilelab.witboost.datacatalogplugin.collibra.model.witboost;

import static org.junit.jupiter.api.Assertions.*;

import it.agilelab.witboost.datacatalogplugin.collibra.common.DataCatalogPluginValidationException;
import it.agilelab.witboost.datacatalogplugin.collibra.config.JaywayJsonPathConfigurer;
import it.agilelab.witboost.datacatalogplugin.collibra.parser.Parser;
import it.agilelab.witboost.datacatalogplugin.collibra.utils.ResourceUtils;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DataProductTest {

    @BeforeAll
    static void setup() {
        JaywayJsonPathConfigurer.configureJaywayJsonPath();
    }

    @Test
    void getComponentKindToProvisionExists() throws IOException {
        var yaml = ResourceUtils.getContentFromResource("/descriptor.yaml");
        var dataProduct = Parser.parseDataProduct(yaml).get();

        var kind = dataProduct.getComponentKindToProvision("urn:dmb:cmp:finance:test-collibra:0:collibra-output-port");

        assertTrue(kind.isDefined());
        assertEquals("outputport", kind.get());
    }

    @Test
    void getComponentKindToProvisionNotExists() throws IOException {
        var yaml = ResourceUtils.getContentFromResource("/descriptor.yaml");
        var dataProduct = Parser.parseDataProduct(yaml).get();

        var kind = dataProduct.getComponentKindToProvision("inexistent:urn");

        assertTrue(kind.isEmpty());
    }

    @Test
    void getComponentToProvisionExists() throws IOException {
        var yaml = ResourceUtils.getContentFromResource("/descriptor.yaml");
        var dataProduct = Parser.parseDataProduct(yaml).get();

        var componentToProvision =
                dataProduct.getComponentToProvision("urn:dmb:cmp:finance:test-collibra:0:collibra-output-port");

        assertTrue(componentToProvision.isDefined());
        assertEquals(
                "urn:dmb:cmp:finance:test-collibra:0:collibra-output-port",
                componentToProvision.get().get("id").textValue());
    }

    @Test
    void getComponentToProvisionNotExists() throws IOException {
        var yaml = ResourceUtils.getContentFromResource("/descriptor.yaml");
        var dataProduct = Parser.parseDataProduct(yaml).get();

        var componentToProvision = dataProduct.getComponentToProvision("inexistent:run");

        assertTrue(componentToProvision.isEmpty());
    }

    @Test
    void extractOutputPorts() throws IOException {
        var yaml = ResourceUtils.getContentFromResource("/descriptor.yaml");
        var dataProduct = Parser.parseDataProduct(yaml).get();

        var outputPorts = dataProduct.extractOutputPorts();

        Assertions.assertEquals(1, outputPorts.size());
        Assertions.assertEquals(
                "urn:dmb:cmp:finance:test-collibra:0:collibra-output-port",
                outputPorts.get(0).getId());
        Assertions.assertTrue(
                dataProduct.getComponents().contains(outputPorts.get(0).getRawComponent()));
    }

    @Test
    void getStringFromJsonPath() throws IOException {
        var yaml = ResourceUtils.getContentFromResource("/descriptor.yaml");
        var dataProduct = Parser.parseDataProduct(yaml).get();

        var simpleQuery = dataProduct.getStringFromJsonPath("$.name");
        var complexQuery = dataProduct.getStringFromJsonPath(
                "$.components.[?(@.id == 'urn:dmb:cmp:finance:test-collibra:0:collibra-storage-area')].name");

        assertEquals("data-product", simpleQuery);
        assertEquals("Collibra Storage Area", complexQuery);
        assertThrows(
                DataCatalogPluginValidationException.class,
                () -> dataProduct.getStringFromJsonPath("$.inexistentField"));
    }
}
