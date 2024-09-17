package it.agilelab.witboost.datacatalogplugin.collibra.model.witboost;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import it.agilelab.witboost.datacatalogplugin.collibra.parser.JsonPathUtils;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "kind",
        visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = OutputPort.class, name = "outputport"),
    @JsonSubTypes.Type(value = StorageArea.class, name = "storage")
})
public abstract class Component<T> {

    protected String id;
    protected String name;
    protected Optional<String> fullyQualifiedName;
    protected String description;
    protected String kind;

    @JsonIgnore
    protected JsonNode rawComponent;

    public String getStringFromJsonPath(String jsonPath) {
        return JsonPathUtils.getStringFromJsonPath(this.rawComponent, jsonPath);
    }
}
