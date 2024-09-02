package it.agilelab.witboost.datacatalogplugin.collibra.service.client;

import static it.agilelab.witboost.datacatalogplugin.collibra.common.TestFixtures.defaultTestConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import it.agilelab.witboost.datacatalogplugin.collibra.client.api.AssetsApi;
import it.agilelab.witboost.datacatalogplugin.collibra.client.api.AttributesApi;
import it.agilelab.witboost.datacatalogplugin.collibra.client.api.RelationsApi;
import it.agilelab.witboost.datacatalogplugin.collibra.client.model.*;
import it.agilelab.witboost.datacatalogplugin.collibra.config.CollibraConfig;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.DataProduct;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.DomainGroup;
import it.agilelab.witboost.datacatalogplugin.collibra.parser.Parser;
import it.agilelab.witboost.datacatalogplugin.collibra.utils.ResourceUtils;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataProductCollibraApiClientTest {

    @Spy
    CollibraConfig collibraConfig = defaultTestConfig;

    @Mock
    AssetsApi assetsApiClient;

    @Mock
    AttributesApi attributesApiClient;

    @Mock
    RelationsApi relationsApiClient;

    @Mock
    DomainCollibraApiClient domainCollibraApiClient;

    @InjectMocks
    DataProductCollibraApiClient client;

    @Test
    void findAssetForDataProductExists() {
        var dataProduct = new DataProduct();
        dataProduct.setName("data-product");
        dataProduct.setId("urn:dmb:...:an-id");
        var dataProductId = UUID.fromString("12345678-1234-1234-1234-1234567890ab");

        var expected = new Asset().name("data-product").id(dataProductId);

        when(assetsApiClient.findAssets(
                        0, 10, dataProduct.getId(), "EXACT", null, null, null, null, null, null, null, null, null))
                .thenReturn(new AssetPagedResponse().addResultsItem(expected));

        var actual = client.findAssetForDataProduct(dataProduct);

        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get());
    }

    @Test
    void findAssetForDataProductNotExists() {
        var dataProduct = new DataProduct();
        dataProduct.setName("data-product");
        dataProduct.setId("urn:dmb:...:an-id");

        when(assetsApiClient.findAssets(
                        0, 10, dataProduct.getId(), "EXACT", null, null, null, null, null, null, null, null, null))
                .thenReturn(new AssetPagedResponse());

        var actual = client.findAssetForDataProduct(dataProduct);

        assertTrue(actual.isEmpty());
    }

    @Test
    void upsertDataProductExists() throws IOException {
        var tagsList = List.of("tag-1", "tag-2");
        var dataProduct = Parser.parseDataProduct(ResourceUtils.getContentFromResource("/descriptor.yaml"))
                .get();

        var communityId = UUID.fromString("00000000-1234-1234-1234-1234567890ab");
        var dataAssetDomainId = UUID.fromString("cafecafe-1234-1234-1234-1234567890ab");
        var expectedCommunity = new Community().name("finance").id(communityId);
        var expectedDataAssetDomain = new Domain().name("Data Products").id(dataAssetDomainId);
        var expectedGlossary = new Domain().name("Glossary");
        var domain = new DomainGroup(expectedCommunity, expectedDataAssetDomain, expectedGlossary);

        // Upsert domain asset
        when(domainCollibraApiClient.upsertDomain("finance")).thenReturn(domain);

        // Search for asset
        var dataProductId = UUID.fromString("12345678-1234-1234-1234-1234567890ab");
        var expectedDPAsset = new Asset().name(dataProduct.getName()).id(dataProductId);
        when(assetsApiClient.findAssets(
                        0, 10, dataProduct.getId(), "EXACT", null, null, null, null, null, null, null, null, null))
                .thenReturn(new AssetPagedResponse().addResultsItem(expectedDPAsset));

        // Change asset request
        when(assetsApiClient.changeAsset(
                        dataProductId,
                        new ChangeAssetRequest()
                                .name(dataProduct.getId())
                                .displayName(dataProduct.getName())
                                .statusId(UUID.fromString("00000000-0000-0000-0000-000000005008"))
                                .domainId(dataAssetDomainId)
                                .typeId(UUID.fromString("00000000-0000-0000-0000-000000031002"))
                                .excludedFromAutoHyperlinking(true)))
                .thenReturn(expectedDPAsset);

        // UpdateDPAttributes
        // set tags
        lenient()
                .when(assetsApiClient.setTagsForAsset(dataProductId, new SetAssetTagsRequest().tagNames(tagsList)))
                .thenReturn(List.of());
        // findAttributes
        when(attributesApiClient.findAttributes(eq(0), eq(1000), eq(null), any(), eq(null), eq(null)))
                .thenReturn(new AttributePagedResponse());
        // remove attributes
        doNothing().when(attributesApiClient).removeAttributes(any());
        // set attributes
        lenient()
                .when(attributesApiClient.addAttribute(new AddAttributeRequest()
                        .assetId(dataProductId)
                        .typeId(UUID.fromString("00000000-0000-0000-0000-000000003114"))
                        .value("description")))
                .thenReturn(new Attribute());

        // updateOutputPorts
        var outputPortAssetId = UUID.fromString("01010101-1234-1234-1234-1234567890ab");
        var columnAssetId = UUID.fromString("9999999-1234-1234-1234-1234567890ab");
        // remove OutputPorts

        // find DP relations
        lenient()
                .when(relationsApiClient.findRelations(
                        0, 1000, UUID.fromString("00000000-0000-0000-0000-000000007017"), dataProductId, null, null))
                .thenReturn(new RelationPagedResponse()
                        .addResultsItem(new Relation().target(new NamedResourceReference().id(outputPortAssetId))));
        // find OP relations
        lenient()
                .when(relationsApiClient.findRelations(0, 1000, null, outputPortAssetId, null, null))
                .thenReturn(new RelationPagedResponse()
                        .addResultsItem(new Relation().target(new NamedResourceReference().id(columnAssetId))));
        // remove OP relations
        doNothing().when(relationsApiClient).removeRelation1(any());
        // remove columns
        doNothing().when(assetsApiClient).removeAsset(any());
        // remove DP relation
        // see above mock
        // remove OP
        // see above mock

        // create OutputPorts
        var outputPort = dataProduct.extractOutputPorts().get(0);
        var outputPortAsset = new Asset().id(outputPortAssetId);
        // add OP
        lenient()
                .when(assetsApiClient.addAsset(new AddAssetRequest()
                        .domainId(dataAssetDomainId)
                        .name(outputPort.getId())
                        .displayName(outputPort.getName())
                        .typeId(UUID.fromString(
                                collibraConfig.assets().outputPort().typeId()))
                        .excludedFromAutoHyperlinking(true)))
                .thenReturn(outputPortAsset);
        // updateOutputPortAttributes

        // set tags
        var opTagsList = List.of("op-tag-1", "op-tag-2");
        lenient()
                .when(assetsApiClient.setTagsForAsset(
                        outputPortAssetId, new SetAssetTagsRequest().tagNames(opTagsList)))
                .thenReturn(List.of());
        // findAttributes
        // see above mock
        // remove attributes
        // see above mock
        // set attributes
        lenient()
                .when(attributesApiClient.addAttribute(new AddAttributeRequest()
                        .assetId(outputPortAssetId)
                        .typeId(UUID.fromString("00000000-0000-0000-0000-000000003114"))
                        .value("description")))
                .thenReturn(new Attribute());
        lenient()
                .when(attributesApiClient.addAttribute(new AddAttributeRequest()
                        .assetId(outputPortAssetId)
                        .typeId(UUID.fromString("00000000-0000-0000-0001-000500000008"))
                        .value("SQL")))
                .thenReturn(new Attribute());

        // create DP-OP relation
        lenient()
                .when(relationsApiClient.addRelation(new AddRelationRequest()
                        .sourceId(dataProductId)
                        .targetId(outputPortAsset.getId())
                        .typeId(UUID.fromString("00000000-0000-0000-0000-000000007017"))))
                .thenReturn(new Relation().target(new NamedResourceReference().id(outputPortAssetId)));
        // foreach column
        var columnAsset = new Asset().id(columnAssetId);
        // create column
        lenient()
                .when(assetsApiClient.addAsset(new AddAssetRequest()
                        .domainId(dataAssetDomainId)
                        .name("urn:dmb:cmp:finance:test-collibra:0:collibra-output-port:date")
                        .displayName("Output Port Name > date")
                        .typeId(UUID.fromString("00000000-0000-0000-0000-000000031008"))
                        .excludedFromAutoHyperlinking(true)))
                .thenReturn(columnAsset);
        // update Column Attributes

        // set tags
        var columnTagsList = List.of("Network Status", "PII");
        lenient()
                .when(assetsApiClient.setTagsForAsset(
                        columnAssetId, new SetAssetTagsRequest().tagNames(columnTagsList)))
                .thenReturn(List.of());
        // findAttributes
        // see above mock
        // remove attributes
        // see above mock
        // set attributes
        lenient()
                .when(attributesApiClient.addAttribute(new AddAttributeRequest()
                        .assetId(columnAssetId)
                        .typeId(UUID.fromString("00000000-0000-0000-0000-000000003114"))
                        .value("description")))
                .thenReturn(new Attribute());
        lenient()
                .when(attributesApiClient.addAttribute(new AddAttributeRequest()
                        .assetId(columnAssetId)
                        .typeId(UUID.fromString("00000000-0000-0000-0000-000000000219"))
                        .value("DATE")))
                .thenReturn(new Attribute());
        // add OP-column relation
        lenient()
                .when(relationsApiClient.addRelation(new AddRelationRequest()
                        .sourceId(outputPortAsset.getId())
                        .targetId(columnAsset.getId())
                        .typeId(UUID.fromString("00000000-0000-0000-0000-000000007062"))))
                .thenReturn(new Relation().target(new NamedResourceReference().id(columnAssetId)));

        assertDoesNotThrow(() -> client.upsertDataProduct(dataProduct));
    }
}
