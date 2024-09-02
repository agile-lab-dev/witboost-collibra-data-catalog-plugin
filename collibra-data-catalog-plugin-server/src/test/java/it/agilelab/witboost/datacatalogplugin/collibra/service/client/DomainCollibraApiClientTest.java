package it.agilelab.witboost.datacatalogplugin.collibra.service.client;

import static it.agilelab.witboost.datacatalogplugin.collibra.common.TestFixtures.defaultTestConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import it.agilelab.witboost.datacatalogplugin.collibra.client.api.CommunitiesApi;
import it.agilelab.witboost.datacatalogplugin.collibra.client.api.DomainsApi;
import it.agilelab.witboost.datacatalogplugin.collibra.client.model.*;
import it.agilelab.witboost.datacatalogplugin.collibra.config.CollibraConfig;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.DomainGroup;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DomainCollibraApiClientTest {

    @Spy
    CollibraConfig collibraConfig = defaultTestConfig;

    @Mock
    CommunitiesApi communitiesApiClient;

    @Mock
    DomainsApi domainsApiClient;

    @InjectMocks
    DomainCollibraApiClient client;

    @Test
    void findCommunityByNameExists() {
        var name = "finance";

        var expected = new Community().name(name);
        when(communitiesApiClient.findCommunities(
                        0,
                        10,
                        name,
                        "EXACT",
                        UUID.fromString("00000000-0000-0000-0001-000100000001"),
                        null,
                        null,
                        null))
                .thenReturn(new CommunityPagedResponse().results(List.of(expected)));

        var actual = client.findCommunityByName(name);
        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get());
    }

    @Test
    void findCommunityByNameNotExists() {
        var name = "finance";

        when(communitiesApiClient.findCommunities(
                        0,
                        10,
                        name,
                        "EXACT",
                        UUID.fromString("00000000-0000-0000-0001-000100000001"),
                        null,
                        null,
                        null))
                .thenReturn(new CommunityPagedResponse().results(List.of()));

        var actual = client.findCommunityByName(name);
        assertTrue(actual.isEmpty());
    }

    @Test
    void findDomainByNameExistsComplete() {
        var name = "finance";
        var communityId = UUID.fromString("12345678-1234-1234-1234-1234567890ab");

        var expectedCommunity = new Community().name(name).id(communityId);
        var expectedDataAssetDomain = new Domain().name("Data Products");
        var expectedGlossary = new Domain().name("Glossary");

        when(communitiesApiClient.findCommunities(
                        0,
                        10,
                        name,
                        "EXACT",
                        UUID.fromString("00000000-0000-0000-0001-000100000001"),
                        null,
                        null,
                        null))
                .thenReturn(new CommunityPagedResponse().results(List.of(expectedCommunity)));
        lenient()
                .when(domainsApiClient.findDomains(
                        0,
                        10,
                        "Data Products",
                        "EXACT",
                        null,
                        communityId,
                        UUID.fromString("00000000-0000-0000-0000-000000030001"),
                        null))
                .thenReturn(new DomainPagedResponse().addResultsItem(expectedDataAssetDomain));
        lenient()
                .when(domainsApiClient.findDomains(
                        0,
                        10,
                        "Glossary",
                        "EXACT",
                        null,
                        communityId,
                        UUID.fromString("00000000-0000-0000-0000-000000010001"),
                        null))
                .thenReturn(new DomainPagedResponse().addResultsItem(expectedGlossary));

        var expected = new DomainGroup(expectedCommunity, expectedDataAssetDomain, expectedGlossary);

        var actual = client.findDomainByName(name);

        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get());
    }

    @Test
    void findDomainByNameExistsIncomplete() {
        var name = "finance";
        var communityId = UUID.fromString("12345678-1234-1234-1234-1234567890ab");

        var expectedCommunity = new Community().name(name).id(communityId);
        var expectedDataAssetDomain = new Domain().name("Data Products");

        when(communitiesApiClient.findCommunities(
                        0,
                        10,
                        name,
                        "EXACT",
                        UUID.fromString("00000000-0000-0000-0001-000100000001"),
                        null,
                        null,
                        null))
                .thenReturn(new CommunityPagedResponse().results(List.of(expectedCommunity)));
        lenient()
                .when(domainsApiClient.findDomains(
                        0,
                        10,
                        "Data Products",
                        "EXACT",
                        null,
                        communityId,
                        UUID.fromString("00000000-0000-0000-0000-000000030001"),
                        null))
                .thenReturn(new DomainPagedResponse().addResultsItem(expectedDataAssetDomain));
        lenient()
                .when(domainsApiClient.findDomains(
                        0,
                        10,
                        "Glossary",
                        "EXACT",
                        null,
                        communityId,
                        UUID.fromString("00000000-0000-0000-0000-000000010001"),
                        null))
                .thenReturn(new DomainPagedResponse());

        var actual = client.findDomainByName(name);

        assertTrue(actual.isEmpty());
    }

    @Test
    void findDomainByNameNotExists() {
        var name = "finance";
        var communityId = UUID.fromString("12345678-1234-1234-1234-1234567890ab");

        when(communitiesApiClient.findCommunities(
                        0,
                        10,
                        name,
                        "EXACT",
                        UUID.fromString("00000000-0000-0000-0001-000100000001"),
                        null,
                        null,
                        null))
                .thenReturn(new CommunityPagedResponse());

        var actual = client.findDomainByName(name);

        assertTrue(actual.isEmpty());
    }

    @Test
    void upsertDomainExists() {
        var name = "finance";
        var communityId = UUID.fromString("12345678-1234-1234-1234-1234567890ab");

        var expectedCommunity = new Community().name(name).id(communityId);
        var expectedDataAssetDomain = new Domain().name("Data Products");
        var expectedGlossary = new Domain().name("Glossary");

        when(communitiesApiClient.findCommunities(
                        0,
                        10,
                        name,
                        "EXACT",
                        UUID.fromString("00000000-0000-0000-0001-000100000001"),
                        null,
                        null,
                        null))
                .thenReturn(new CommunityPagedResponse().results(List.of(expectedCommunity)));
        lenient()
                .when(domainsApiClient.findDomains(
                        0,
                        10,
                        "Data Products",
                        "EXACT",
                        null,
                        communityId,
                        UUID.fromString("00000000-0000-0000-0000-000000030001"),
                        null))
                .thenReturn(new DomainPagedResponse().addResultsItem(expectedDataAssetDomain));
        lenient()
                .when(domainsApiClient.findDomains(
                        0,
                        10,
                        "Glossary",
                        "EXACT",
                        null,
                        communityId,
                        UUID.fromString("00000000-0000-0000-0000-000000010001"),
                        null))
                .thenReturn(new DomainPagedResponse().addResultsItem(expectedGlossary));

        when(communitiesApiClient.changeCommunity(communityId, new ChangeCommunityRequest().name("finance")))
                .thenReturn(expectedCommunity);

        var expected = new DomainGroup(expectedCommunity, expectedDataAssetDomain, expectedGlossary);

        var actual = client.upsertDomain("finance");

        assertEquals(expected, actual);
    }

    @Test
    void upsertDomainNotExists() {
        var name = "finance";
        var baseCommunityId = UUID.fromString("00000000-0000-0000-0001-000100000001");
        var communityId = UUID.fromString("12345678-1234-1234-1234-1234567890ab");

        var expectedCommunity = new Community().name(name).id(communityId);
        var expectedDataAssetDomain = new Domain().name("Data Products");
        var expectedGlossary = new Domain().name("Glossary");

        when(communitiesApiClient.findCommunities(0, 10, name, "EXACT", baseCommunityId, null, null, null))
                .thenReturn(new CommunityPagedResponse());

        when(communitiesApiClient.addCommunity(
                        new AddCommunityRequest().name(name).parentId(baseCommunityId)))
                .thenReturn(expectedCommunity);

        // Setting domainType as nullable because these are called used on both findDomain and findCollibraDomain
        lenient()
                .when(domainsApiClient.findDomains(
                        eq(0),
                        eq(10),
                        eq("Data Products"),
                        eq("EXACT"),
                        eq(null),
                        eq(communityId),
                        nullable(UUID.class),
                        eq(null)))
                .thenReturn(new DomainPagedResponse());
        lenient()
                .when(domainsApiClient.addDomain(new AddDomainRequest()
                        .name("Data Products")
                        .typeId(UUID.fromString("00000000-0000-0000-0000-000000030001"))
                        .communityId(communityId)
                        .excludedFromAutoHyperlinking(true)))
                .thenReturn(expectedDataAssetDomain);

        lenient()
                .when(domainsApiClient.findDomains(
                        eq(0),
                        eq(10),
                        eq("Glossary"),
                        eq("EXACT"),
                        eq(null),
                        eq(communityId),
                        nullable(UUID.class),
                        eq(null)))
                .thenReturn(new DomainPagedResponse());
        lenient()
                .when(domainsApiClient.addDomain(new AddDomainRequest()
                        .name("Glossary")
                        .typeId(UUID.fromString("00000000-0000-0000-0000-000000010001"))
                        .communityId(communityId)
                        .excludedFromAutoHyperlinking(true)))
                .thenReturn(expectedGlossary);

        var expected = new DomainGroup(expectedCommunity, expectedDataAssetDomain, expectedGlossary);

        var actual = client.upsertDomain("finance");

        assertEquals(expected, actual);
    }

    @Test
    void upsertDomainIncomplete() {
        var name = "finance";
        var baseCommunityId = UUID.fromString("00000000-0000-0000-0001-000100000001");
        var communityId = UUID.fromString("12345678-1234-1234-1234-1234567890ab");

        var expectedCommunity = new Community().name(name).id(communityId);
        var expectedDataAssetDomain = new Domain().name("Data Products");
        var expectedGlossary = new Domain().name("Glossary");

        when(communitiesApiClient.findCommunities(0, 10, name, "EXACT", baseCommunityId, null, null, null))
                .thenReturn(new CommunityPagedResponse().addResultsItem(expectedCommunity));

        // Setting domainType as nullable because these are called used on both findDomain and findCollibraDomain
        lenient()
                .when(domainsApiClient.findDomains(
                        eq(0),
                        eq(10),
                        eq("Data Products"),
                        eq("EXACT"),
                        eq(null),
                        eq(communityId),
                        nullable(UUID.class),
                        eq(null)))
                .thenReturn(new DomainPagedResponse().addResultsItem(expectedDataAssetDomain));

        lenient()
                .when(domainsApiClient.findDomains(
                        eq(0),
                        eq(10),
                        eq("Glossary"),
                        eq("EXACT"),
                        eq(null),
                        eq(communityId),
                        nullable(UUID.class),
                        eq(null)))
                .thenReturn(new DomainPagedResponse());
        lenient()
                .when(domainsApiClient.addDomain(new AddDomainRequest()
                        .name("Glossary")
                        .typeId(UUID.fromString("00000000-0000-0000-0000-000000010001"))
                        .communityId(communityId)
                        .excludedFromAutoHyperlinking(true)))
                .thenReturn(expectedGlossary);

        var expected = new DomainGroup(expectedCommunity, expectedDataAssetDomain, expectedGlossary);

        var actual = client.upsertDomain("finance");

        assertEquals(expected, actual);
    }
}
