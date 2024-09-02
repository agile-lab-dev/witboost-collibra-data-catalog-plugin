package it.agilelab.witboost.datacatalogplugin.collibra.model.witboost;

import static it.agilelab.witboost.datacatalogplugin.collibra.common.Constants.OUTPUTPORT_KIND;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Either;
import io.vavr.control.Option;
import it.agilelab.witboost.datacatalogplugin.collibra.parser.Parser;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataProduct {

    private String id;
    private String name;
    private Optional<String> fullyQualifiedName;
    private String description;
    private String kind;
    private String domain;
    private String version;
    private String environment;
    private String dataProductOwner;
    private String dataProductOwnerDisplayName;
    private Optional<String> email;
    private String ownerGroup;
    private String devGroup;
    private Optional<String> informationSLA;
    private Optional<String> status;
    private Optional<String> maturity;
    private Optional<JsonNode> billing;
    private List<Tag> tags = List.of();
    private JsonNode specific;
    private List<JsonNode> components;

    public Option<JsonNode> getComponentToProvision(String componentId) {
        return Option.ofOptional(Optional.ofNullable(componentId).flatMap(comp -> components.stream()
                .filter(c -> comp.equals(c.get("id").textValue()))
                .findFirst()));
    }

    public Option<String> getComponentKindToProvision(String componentId) {
        return Option.ofOptional(Optional.ofNullable(componentId).flatMap(comp -> components.stream()
                .filter(c -> comp.equals(c.get("id").textValue()))
                .findFirst()
                .flatMap(c -> Optional.ofNullable(c.get("kind")))
                .map(JsonNode::textValue)));
    }

    public List<OutputPort<Specific>> extractOutputPorts() {
        // TODO Should we return error on malformed output port? Or assume everything is OK since DC is called at the
        //  end of the line?
        return this.getComponents().stream()
                .filter(component -> component.get("kind").asText("none").equals(OUTPUTPORT_KIND))
                .map(outputport -> Parser.parseComponent(outputport, Specific.class))
                .filter(Either::isRight)
                .map(Either::get)
                .map(x -> (OutputPort<Specific>) x)
                .toList();
    }
}
