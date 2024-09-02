package it.agilelab.witboost.datacatalogplugin.collibra.service.client;

import static it.agilelab.witboost.datacatalogplugin.collibra.common.TestFixtures.defaultTestConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.vavr.control.Option;
import it.agilelab.witboost.datacatalogplugin.collibra.client.api.AssetsApi;
import it.agilelab.witboost.datacatalogplugin.collibra.client.model.Asset;
import it.agilelab.witboost.datacatalogplugin.collibra.client.model.AssetPagedResponse;
import it.agilelab.witboost.datacatalogplugin.collibra.client.model.Domain;
import it.agilelab.witboost.datacatalogplugin.collibra.config.CollibraConfig;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.DomainGroup;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;

@ExtendWith(MockitoExtension.class)
class BusinessTermCollibraApiClientTest {

    @Spy
    CollibraConfig collibraConfig = defaultTestConfig;

    @Mock
    AssetsApi assetsApiClient;

    @InjectMocks
    BusinessTermCollibraApiClient client;

    @Test
    void findBusinessTermAssetsNoDomain() {
        var returnValue = List.of(new Asset().name("my-business-term"), new Asset().name("my-business-term2"));
        var offset = 0;
        var limit = 10;
        var name = "my-business";

        lenient()
                .when(assetsApiClient.findAssets(
                        offset,
                        limit,
                        name,
                        "ANYWHERE",
                        null,
                        null,
                        List.of(UUID.fromString("00000000-0000-0000-0000-000000011001")),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null))
                .thenReturn(new AssetPagedResponse().results(returnValue));

        var actual = client.findBusinessTermAssets(
                BigInteger.ZERO, BigInteger.valueOf(10), Option.of(name), Optional.empty());

        assertEquals(actual, returnValue);
    }

    @Test
    void findBusinessTermAssetsWithDomain() {
        var returnValue = List.of(new Asset().name("my-business-term"), new Asset().name("my-business-term2"));
        var offset = 0;
        var limit = 10;
        var name = "my-business";
        var domain =
                new DomainGroup(null, null, new Domain().id(UUID.fromString("12345678-1234-1234-1234-1234567890ab")));

        lenient()
                .when(assetsApiClient.findAssets(
                        offset,
                        limit,
                        name,
                        "ANYWHERE",
                        UUID.fromString("12345678-1234-1234-1234-1234567890ab"),
                        null,
                        List.of(UUID.fromString("00000000-0000-0000-0000-000000011001")),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null))
                .thenReturn(new AssetPagedResponse().results(returnValue));

        var actual = client.findBusinessTermAssets(
                BigInteger.ZERO, BigInteger.valueOf(10), Option.of(name), Optional.of(domain));

        assertEquals(actual, returnValue);
    }

    @Test
    void findBusinessTermAssetExists() {
        var id = "12345678-1234-1234-1234-1234567890ab";
        var expected = new Asset().id(UUID.fromString(id));
        doReturn(expected).when(assetsApiClient).getAsset(UUID.fromString(id));

        var actual = client.findBusinessTermAsset(id);

        assertTrue(actual.isDefined());
        assertEquals(expected, actual.get());
    }

    @Test
    void findBusinessTermAssetNotExists() {
        var id = "12345678-1234-1234-1234-1234567890ab";
        doThrow(new HttpClientErrorException(HttpStatusCode.valueOf(404)))
                .when(assetsApiClient)
                .getAsset(UUID.fromString(id));

        var actual = client.findBusinessTermAsset(id);

        assertTrue(actual.isEmpty());
    }
}
