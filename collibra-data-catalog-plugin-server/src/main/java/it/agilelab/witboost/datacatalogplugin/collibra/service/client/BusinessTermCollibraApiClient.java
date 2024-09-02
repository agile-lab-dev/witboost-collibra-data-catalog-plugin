package it.agilelab.witboost.datacatalogplugin.collibra.service.client;

import io.vavr.control.Option;
import it.agilelab.witboost.datacatalogplugin.collibra.client.api.AssetsApi;
import it.agilelab.witboost.datacatalogplugin.collibra.client.model.Asset;
import it.agilelab.witboost.datacatalogplugin.collibra.config.CollibraConfig;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.DomainGroup;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;

public class BusinessTermCollibraApiClient {

    private final CollibraConfig collibraConfig;
    private final AssetsApi assetsApiClient;

    private final Logger logger = LoggerFactory.getLogger(BusinessTermCollibraApiClient.class);

    public BusinessTermCollibraApiClient(CollibraConfig collibraConfig, AssetsApi assetsApiClient) {
        this.collibraConfig = collibraConfig;
        this.assetsApiClient = assetsApiClient;
    }

    public List<Asset> findBusinessTermAssets(
            BigInteger offset, BigInteger limit, Option<String> nameFilter, Optional<DomainGroup> domain) {
        var nameSearch = nameFilter.getOrElse((String) null);
        var businessTermAssetTypeId = collibraConfig.assets().businessTerm().typeId();
        var response = assetsApiClient.findAssets(
                offset.intValue(),
                limit.intValue(),
                nameSearch,
                "ANYWHERE",
                domain.map(domainGroup -> domainGroup.glossary().getId()).orElse(null),
                null,
                List.of(UUID.fromString(businessTermAssetTypeId)),
                null,
                null,
                null,
                null,
                null,
                null);
        return response.getResults();
    }

    public Option<Asset> findBusinessTermAsset(String id) {
        logger.debug("Contacting Collibra to find Business Term asset with id '{}", id);
        try {
            var response = assetsApiClient.getAsset(UUID.fromString(id));
            return Option.some(response);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                logger.error(String.format("Couldn't find Business Term asset with id '%s'", id), ex);
                return Option.none();
            } else throw ex;
        }
    }
}
