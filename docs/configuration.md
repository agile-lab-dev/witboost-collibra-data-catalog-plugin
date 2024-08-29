# Collibra Data Catalog Plugin Configuration

Application configuration is handled using the features provided by Spring Boot. You can find the default settings in the `application.yml`. Customize it and use the `spring.config.location` system property or the other options provided by the framework according to your needs.

### Collibra configuration

The Data Catalog Plugin needs to be configured in order to authenticate against a Collibra environment and to manage the correct domains and assets. It also allows to configure the base Community where the plugin expects everything to be stored under (see [High Level Design](HLD.md)).

#### Authentication

| Configuration  | Description                                                                                  | Default              |
|:---------------|:---------------------------------------------------------------------------------------------|:---------------------|
| `api.username` | Username to be used to access Collibra and perform all of the Data Catalog plugin operations | ${COLLIBRA_USERNAME} | 
| `api.password` | Password to be used to access Collibra and perform all of the Data Catalog plugin operations | ${COLLIBRA_PASSWORD} | 
| `endpoint`     | Base endpoint of the target Collibra environment                                             | ${COLLIBRA_USERNAME} | 

#### Mapping between Witboost and Collibra

Data Mesh entities in Witboost are mapped to Collibra concepts using a set of type IDs for each of the assets and their attributes. Furhtermore, the Data Catalog plugin expects all the assets and domains to be stored under a common Collibra community (see [High Level Design](HLD.md)), whose ID is also configurable. 

| Configuration                         | Description                                                                       | Default |
|:--------------------------------------|:----------------------------------------------------------------------------------|:--------|
| `collibra.communityId`                | ID of the base Collibra community that will store all domains and assets          |         |  
| `collibra.dataProductTypeId`          | ID of the asset that represents Data Products. Typically a Collibra Data Asset    |         |  
| `collibra.statusId`                   | ID of the initial status to assign to a Collibra asset                            |         |  
| `collibra.domainTypeId`               | ID of the domain type that represent Data Mesh domains. Usually Data Asset Domain |         |  
| `collibra.descriptionAttributeTypeId` | ID that represents the attribute type for the an asset description                |         |  
| `collibra.columnTypeId`               | ID of the asset that represents Output Port schema columns.                       |         |  
| `collibra.relationTypeId`             | ID of the relation type that links Data Products and Output Ports.                |         |  
| `collibra.businessTermTypeId`         | ID of the asset that represents business terms.                                   |         |  


### Custom URL Picker configuration

This microservice provides support for Custom URL Picker endpoints, in order to retrieve a set of business terms based on a configurable asset type ID (see [above](#mapping-between-witboost-and-collibra)), and an input with filters based on text and an optional domain set on the appropriate Witboost template. In order for these endpoints to be reachable by Witboost user interface, we need to allow CORS on the set of the picker endpoints. For this, we provide two configurations in order to set the endpoint mask and the origin URL from which requests will be allowed.

| Configuration                         | Description                         | Default            |
|:--------------------------------------|:------------------------------------|:-------------------|
| `custom-url-picker.cors.endpointMask` | Endpoint mask to allow CORS         | `/v1/resources/**` |
| `custom-url-picker.cors.origin`       | Origin URL allowed on CORS requests |                    |
