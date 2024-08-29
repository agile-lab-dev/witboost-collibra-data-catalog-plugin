package it.agilelab.witboost.datacatalogplugin.collibra.model.witboost;

import io.vavr.control.Option;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.customurlpicker.CustomURLPickerResourcesRequestBody;
import java.math.BigInteger;

public record CustomUrlPickerRequest(
        BigInteger offset,
        BigInteger limit,
        Option<String> filter,
        Option<CustomURLPickerResourcesRequestBody> queryParameters) {

    public CustomUrlPickerRequest(CustomURLPickerResourcesRequestBody queryParameters) {
        this(null, null, Option.none(), Option.of(queryParameters));
    }
}
