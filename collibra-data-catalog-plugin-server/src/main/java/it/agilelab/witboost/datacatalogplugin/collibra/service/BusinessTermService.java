package it.agilelab.witboost.datacatalogplugin.collibra.service;

import io.vavr.control.Option;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.BusinessTerm;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.CustomUrlPickerRequest;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.customurlpicker.CustomURLPickerItem;
import java.util.List;

public interface BusinessTermService {

    List<BusinessTerm> getBusinessTerms(CustomUrlPickerRequest request);

    void validateBusinessTerms(List<CustomURLPickerItem> businessTerms, Option<CustomUrlPickerRequest> request);
}
