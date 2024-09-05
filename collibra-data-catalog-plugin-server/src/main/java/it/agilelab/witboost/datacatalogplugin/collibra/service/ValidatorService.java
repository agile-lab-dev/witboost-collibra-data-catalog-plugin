package it.agilelab.witboost.datacatalogplugin.collibra.service;

import it.agilelab.witboost.datacatalogplugin.collibra.common.FailedOperation;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.DataProduct;
import java.util.Optional;

public interface ValidatorService {

    Optional<FailedOperation> validate(DataProduct dataProduct);
}
