package it.agilelab.witboost.datacatalogplugin.collibra.model.witboost;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import it.agilelab.witboost.datacatalogplugin.collibra.parser.JsonPathUtils;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Column {
    private String name;
    private String description;
    private String dataType;
    private String dataLength;
    private List<Tag> tags = List.of();

    @JsonIgnore
    private JsonNode rawColumn;

    public String getStringFromJsonPath(String jsonPath) {
        return JsonPathUtils.getStringFromJsonPath(this.rawColumn, jsonPath);
    }
}
