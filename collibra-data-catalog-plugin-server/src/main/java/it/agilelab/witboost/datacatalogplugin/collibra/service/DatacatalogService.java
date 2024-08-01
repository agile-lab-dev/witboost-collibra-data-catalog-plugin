package it.agilelab.witboost.datacatalogplugin.collibra.service;

import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.EntityReference;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.ProvisioningRequest;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.ProvisioningStatus;
import it.agilelab.witboost.datacatalogplugin.collibra.openapi.model.ValidationResult;

/***
 * Service to handle data catalog provisioning
 */
public interface DatacatalogService {

    /**
     * Provides an entity reference for a component
     *
     * @param componentId component id for which to return the entity reference
     * @return the entity reference for the component
     */
    EntityReference getEntityReference(String componentId);

    /**
     * Validate the provisioning request
     *
     * @param provisioningRequest request to validate
     * @return the outcome of the validation
     */
    ValidationResult validate(ProvisioningRequest provisioningRequest);

    /**
     * Provision the component present in the request
     *
     * @param provisioningRequest the request
     * @return the outcome of the provision
     */
    ProvisioningStatus provision(ProvisioningRequest provisioningRequest);

    /**
     * Unprovision the component present in the request
     *
     * @param provisioningRequest the request
     * @return the outcome of the unprovision
     */
    ProvisioningStatus unprovision(ProvisioningRequest provisioningRequest);
}
