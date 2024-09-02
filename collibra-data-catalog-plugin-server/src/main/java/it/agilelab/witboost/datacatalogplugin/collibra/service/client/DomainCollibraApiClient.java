package it.agilelab.witboost.datacatalogplugin.collibra.service.client;

import it.agilelab.witboost.datacatalogplugin.collibra.client.api.CommunitiesApi;
import it.agilelab.witboost.datacatalogplugin.collibra.client.api.DomainsApi;
import it.agilelab.witboost.datacatalogplugin.collibra.client.model.*;
import it.agilelab.witboost.datacatalogplugin.collibra.config.CollibraConfig;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.DomainGroup;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomainCollibraApiClient {
    private final CollibraConfig collibraConfig;

    private final CommunitiesApi communitiesApiClient;
    private final DomainsApi domainsApiClient;

    private final Logger logger = LoggerFactory.getLogger(CollibraApiClient.class);

    public DomainCollibraApiClient(
            CollibraConfig collibraConfig, CommunitiesApi communitiesApiClient, DomainsApi domainsApiClient) {
        this.collibraConfig = collibraConfig;
        this.communitiesApiClient = communitiesApiClient;
        this.domainsApiClient = domainsApiClient;
    }

    public Optional<Community> findCommunityByName(String name) {
        logger.info("Querying for Collibra community '{}'", name);
        var response = communitiesApiClient.findCommunities(
                0, 10, name, "EXACT", UUID.fromString(collibraConfig.baseCommunityId()), null, null, null);

        return response.getResults().stream().findFirst();
    }

    public Optional<DomainGroup> findDomainByName(String name) {
        logger.info("Querying for Witboost domain '{}' on Collibra", name);

        var optionalCommunity = findCommunityByName(name);

        return optionalCommunity.flatMap(community -> {
            logger.info("Found root Collibra community '{}' with ID '{}'", community.getName(), community.getId());
            logger.info(
                    "Querying for domain '{}' on community '{}'",
                    collibraConfig.domains().assetDomain().name(),
                    community.getName());
            var dataAssetDomainResponse = domainsApiClient.findDomains(
                    0,
                    10,
                    collibraConfig.domains().assetDomain().name(),
                    "EXACT",
                    null,
                    community.getId(),
                    UUID.fromString(collibraConfig.domains().assetDomain().typeId()),
                    null);
            var optionalDataAsset =
                    dataAssetDomainResponse.getResults().stream().findFirst();

            logger.info(
                    "Querying for Glossary Domain '{}' on community '{}'",
                    collibraConfig.domains().glossary().name(),
                    community.getName());
            var glossaryResponse = domainsApiClient.findDomains(
                    0,
                    10,
                    collibraConfig.domains().glossary().name(),
                    "EXACT",
                    null,
                    community.getId(),
                    UUID.fromString(collibraConfig.domains().glossary().typeId()),
                    null);
            var optionalGlossary = glossaryResponse.getResults().stream().findFirst();

            if (optionalDataAsset.isPresent() && optionalGlossary.isPresent()) {
                var domainGroup = new DomainGroup(community, optionalDataAsset.get(), optionalGlossary.get());
                logger.debug(
                        "Witboost domain '{}' is complete on Collibra wit root community ID '{}'",
                        name,
                        community.getId());
                logger.debug("Witboost domain '{}' is complete on Collibra: {}", name, domainGroup);
                return Optional.of(domainGroup);
            } else {
                logger.warn(
                        "Community with name '{}' exists, but it doesn't contain the Data Asset Domain and/or Glossary domains",
                        name);
                return Optional.empty();
            }
        });
    }

    private Optional<Domain> findCollibraDomainByName(UUID parentCommunityId, String name) {
        logger.info("Querying for Collibra domain '{}'", name);

        logger.info(
                "Querying for domain '{}' on community '{}'",
                collibraConfig.domains().assetDomain().name(),
                parentCommunityId);
        var dataAssetDomainResponse =
                domainsApiClient.findDomains(0, 10, name, "EXACT", null, parentCommunityId, null, null);
        return dataAssetDomainResponse.getResults().stream().findFirst();
    }

    public DomainGroup upsertDomain(String name) {
        logger.info("Upserting Witboost domain with name '{}'", name);
        var maybeDomain = findDomainByName(name);

        if (maybeDomain.isEmpty()) {
            logger.info("Domain '{}' is not present on Collibra environment or is incomplete, creating...", name);
            return createDomain(name);
        } else {
            logger.info("Domain '{}' is present on Collibra environment, updating...", name);
            return updateDomain(maybeDomain.get(), name);
        }
    }

    public DomainGroup createDomain(String name) {
        logger.info("Creating Witboost domain '{}' on Collibra", name);

        logger.debug("Querying for root Collibra community '{}' as it may be present already", name);
        var communityExists = findCommunityByName(name);

        Community community;
        if (communityExists.isEmpty()) {
            logger.info(
                    "Creating root Collibra community with name '{}' under base community '{}'",
                    name,
                    collibraConfig.baseCommunityId());
            var addCommunity =
                    new AddCommunityRequest().name(name).parentId(UUID.fromString(collibraConfig.baseCommunityId()));
            community = communitiesApiClient.addCommunity(addCommunity);
            logger.info("Collibra community with name '{}' created successfully", name);
        } else {
            logger.info("Collibra community already exists on environment, skipping creation");
            community = communityExists.get();
        }

        var optionalDataAssetDomain = findCollibraDomainByName(
                community.getId(), collibraConfig.domains().assetDomain().name());
        Domain dataAssetDomain;
        if (optionalDataAssetDomain.isEmpty()) {
            logger.info(
                    "Creating Domain '{}' for data assets with type ID '{}'",
                    collibraConfig.domains().assetDomain().name(),
                    collibraConfig.domains().assetDomain().typeId());
            var addDataAssetDomainRequest = new AddDomainRequest()
                    .name(collibraConfig.domains().assetDomain().name())
                    .typeId(UUID.fromString(
                            collibraConfig.domains().assetDomain().typeId()))
                    .communityId(community.getId())
                    .excludedFromAutoHyperlinking(true);
            dataAssetDomain = domainsApiClient.addDomain(addDataAssetDomainRequest);
            logger.info(
                    "Collibra domain for data assets with name '{}' created successfully",
                    collibraConfig.domains().assetDomain().name());

        } else {
            logger.info(
                    "Domain for data assets on Community '{}' already exists. Skipping creation", community.getName());
            dataAssetDomain = optionalDataAssetDomain.get();
        }

        var optionalGlossary = findCollibraDomainByName(
                community.getId(), collibraConfig.domains().glossary().name());
        Domain glossary;
        if (optionalGlossary.isEmpty()) {
            logger.info(
                    "Creating Glossary '{}' for business terms with type ID '{}'",
                    collibraConfig.domains().glossary().name(),
                    collibraConfig.domains().glossary().typeId());
            var addGlossaryRequest = new AddDomainRequest()
                    .name(collibraConfig.domains().glossary().name())
                    .typeId(UUID.fromString(collibraConfig.domains().glossary().typeId()))
                    .communityId(community.getId())
                    .excludedFromAutoHyperlinking(true);
            glossary = domainsApiClient.addDomain(addGlossaryRequest);
            logger.info(
                    "Collibra glossary domain with name '{}' created successfully",
                    collibraConfig.domains().glossary().name());
        } else {
            logger.info("Glossary on Community '{}' already exists. Skipping creation", community.getName());
            glossary = optionalGlossary.get();
        }

        return new DomainGroup(community, dataAssetDomain, glossary);
    }

    public DomainGroup updateDomain(DomainGroup domainGroup, String name) {
        logger.info("Updating Collibra community linked to Witboost domain '{}'", name);
        var changeCommunityRequest = new ChangeCommunityRequest().name(name);

        var updatedCommunity =
                communitiesApiClient.changeCommunity(domainGroup.rootDomain().getId(), changeCommunityRequest);
        logger.info("Collibra community linked to Witboost domain '{}' updated successfully", name);

        return new DomainGroup(updatedCommunity, domainGroup.dataAssetDomain(), domainGroup.glossary());
    }
}
