package it.agilelab.witboost.datacatalogplugin.collibra.service;

import static org.junit.jupiter.api.Assertions.*;

import it.agilelab.witboost.datacatalogplugin.collibra.parser.Parser;
import it.agilelab.witboost.datacatalogplugin.collibra.utils.ResourceUtils;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CollibraValidatorServiceTest {

    CollibraValidatorService collibraValidatorService = new CollibraValidatorService();

    @Test
    void validateIsValid() throws IOException {
        var yamlString = ResourceUtils.getContentFromResource("/descriptor.yaml");
        var dataProduct = Parser.parseDataProduct(yamlString).get();

        var actual = collibraValidatorService.validate(dataProduct);

        Assertions.assertTrue(actual.isEmpty());
    }

    @Test
    void validateReturnsErrorOnWhitespaceTags() throws IOException {
        var yamlString = ResourceUtils.getContentFromResource("/descriptor_whitespace_tags.yaml");
        var dataProduct = Parser.parseDataProduct(yamlString).get();

        var actual = collibraValidatorService.validate(dataProduct);

        Assertions.assertTrue(actual.isPresent());
        Assertions.assertEquals(actual.get().problems().size(), 5); // 2 on DP, 2 on OP, 1 on column
        actual.get().problems().forEach(p -> {
            Assertions.assertTrue(p.description().startsWith("Tag"));
            Assertions.assertTrue(p.description().endsWith("cannot have whitespace"));
        });
    }
}
