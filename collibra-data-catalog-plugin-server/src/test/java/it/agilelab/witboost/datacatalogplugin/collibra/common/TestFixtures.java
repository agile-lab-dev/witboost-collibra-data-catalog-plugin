package it.agilelab.witboost.datacatalogplugin.collibra.common;

import it.agilelab.witboost.datacatalogplugin.collibra.config.AssetConfig;
import it.agilelab.witboost.datacatalogplugin.collibra.config.CollibraAssetsConfig;
import it.agilelab.witboost.datacatalogplugin.collibra.config.CollibraConfig;
import it.agilelab.witboost.datacatalogplugin.collibra.config.CollibraDomainsConfig;
import jakarta.validation.ConstraintViolation;
import org.hibernate.validator.internal.engine.ConstraintViolationImpl;
import org.hibernate.validator.internal.engine.path.PathImpl;

public class TestFixtures {
    public static ConstraintViolation<?> buildConstraintViolation(String interpolatedMessage, String path) {
        return ConstraintViolationImpl.forBeanValidation(
                "",
                null,
                null,
                interpolatedMessage,
                null,
                null,
                null,
                null,
                PathImpl.createPathFromString(path),
                null,
                null);
    }

    public static CollibraConfig defaultTestConfig = new CollibraConfig(
            "00000000-0000-0000-0001-000100000001",
            "00000000-0000-0000-0000-000000005008",
            new CollibraDomainsConfig(
                    new CollibraDomainsConfig.CollibraDomainConfig(
                            "Data Products", "00000000-0000-0000-0000-000000030001"),
                    new CollibraDomainsConfig.CollibraDomainConfig("Glossary", "00000000-0000-0000-0000-000000010001")),
            new CollibraAssetsConfig(
                    new CollibraAssetsConfig.DataProductConfig(
                            "00000000-0000-0000-0000-000000031002",
                            "00000000-0000-0000-0000-000000007017",
                            new CollibraAssetsConfig.DataProductConfig.AttributesConfig(
                                    "00000000-0000-0000-0000-000000003114")),
                    new CollibraAssetsConfig.OutputPortConfig(
                            "00000000-0000-0000-0001-000400000001",
                            "00000000-0000-0000-0000-000000007062",
                            new CollibraAssetsConfig.OutputPortConfig.AttributesConfig(
                                    "00000000-0000-0000-0000-000000003114", "00000000-0000-0000-0001-000500000008")),
                    new CollibraAssetsConfig.ColumnConfig(
                            "00000000-0000-0000-0000-000000031008",
                            new CollibraAssetsConfig.ColumnConfig.AttributesConfig(
                                    "00000000-0000-0000-0000-000000003114", "00000000-0000-0000-0000-000000000219")),
                    new AssetConfig("00000000-0000-0000-0000-000000011001")));
}
