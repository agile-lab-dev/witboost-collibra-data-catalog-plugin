package it.agilelab.witboost.datacatalogplugin.collibra.model.witboost;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import it.agilelab.witboost.datacatalogplugin.collibra.parser.JsonPathUtils;
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

    @JsonIgnore
    private JsonNode rawDataContract;

    public void setRawDataContract(JsonNode rawDataContract) {
        this.rawDataContract = rawDataContract;
        var schema = (ArrayNode) rawDataContract.get("schema");
        this.schema.forEach(column -> {
            for (JsonNode jsonColumn : schema) {
                // Check if the node contains a "name" field and if it matches the parameter
                if (jsonColumn.has("name") && jsonColumn.get("name").asText().equals(column.getName())) {
                    column.setRawColumn(jsonColumn);
                }
            }
        });
    }

    public String getStringFromJsonPath(String jsonPath) {
        return JsonPathUtils.getStringFromJsonPath(this.rawDataContract, jsonPath);
    }
}
