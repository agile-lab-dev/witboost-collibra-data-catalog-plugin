package it.agilelab.witboost.datacatalogplugin.collibra.model.witboost;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataContract {
    private List<Column> schema;
    private Optional<String> termsAndConditions;
    private Optional<String> SLA;
    private Optional<String> intervalOfChange;
    private Optional<String> timeliness;
    private Optional<String> upTime;
}
