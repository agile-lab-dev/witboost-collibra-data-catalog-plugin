# Collibra Data Catalog Plugin Configuration

Application configuration is handled using the features provided by Spring Boot. You can find the default settings in the `application.yml`. Customize it and use the `spring.config.location` system property or the other options provided by the framework according to your needs.

### Collibra configuration

The Data Catalog Plugin needs to be configured in order to authenticate against a Collibra environment and to manage the correct domains and assets. It also allows to configure the base Community where the plugin expects everything to be stored under (see [High Level Design](HLD.md)).

#### Authentication

| Configuration  | Description                                                                                  | Default                |
|:---------------|:---------------------------------------------------------------------------------------------|:-----------------------|
| `api.username` | Username to be used to access Collibra and perform all of the Data Catalog plugin operations | `${COLLIBRA_USERNAME}` | 
| `api.password` | Password to be used to access Collibra and perform all of the Data Catalog plugin operations | `${COLLIBRA_PASSWORD}` | 
| `endpoint`     | Base endpoint of the target Collibra environment                                             | `${COLLIBRA_USERNAME}` | 

#### Mapping between Witboost and Collibra

Data Mesh entities in Witboost are mapped to Collibra concepts using a set of type IDs for each of the assets and their attributes. Furhtermore, the Data Catalog plugin expects all the assets and domains to be stored under a common Collibra community (see [High Level Design](HLD.md)), whose ID is also configurable. 

| Configuration              | Description                                                                | Default                                |
|:---------------------------|:---------------------------------------------------------------------------|:---------------------------------------|
| `collibra.baseCommunityId` | ID of the base Collibra community that will store all domains and assets   |                                        |  
| `collibra.initialStatusId` | ID of the initial status to assign to a Collibra asset. Default: Candidate | `00000000-0000-0000-0000-000000005008` |  

##### Witboost Domains

Witboost domains map to a Collibra community containing two domains: A domain for all the assets, and a domain for the business terms. These are all configured on the `collibra.domains` configuration:

| Configuration                         | Description                                                                                                 | Default                                |
|:--------------------------------------|:------------------------------------------------------------------------------------------------------------|:---------------------------------------|
| `collibra.domains.assetDomain.typeId` | ID of the domain type that represent the domain for storing Data Product assets. Default: Data Asset Domain | `00000000-0000-0000-0000-000000030001` |
| `collibra.domains.assetDomain.name`   | Name of the domain for storing Data Product assets.                                                         | `Data Products`                        |
| `collibra.domains.glossary.typeId`    | ID of the domain type that represent the domain for storing business terms. Default: Glossary               | `00000000-0000-0000-0000-000000010001` |
| `collibra.domains.glossary.name`      | Name of the domain for storing business terms.                                                              | `Glossary`                             |


##### Witboost Data Products

Data Products and all their entities are created as different types of assets stored under the Collibra community on the appropriate domain: Data Products are stored on the asset domain configured on `collibra.domains.assetDomain`, while business terms are stored on the glossary domain configured on `collibra.domains.glossary`, all linked to the community linked to the Data Product domain. 

| Configuration                                              | Description                                                                                                | Default                                |
|:-----------------------------------------------------------|:-----------------------------------------------------------------------------------------------------------|:---------------------------------------|
| `collibra.assets.dataProduct.typeId`                       | ID of the asset that represents Data Products. Default: Data Asset                                         | `00000000-0000-0000-0000-000000031002` |
| `collibra.assets.dataProduct.attributes.description`       | ID that represents the attribute type for the Data Product description. Default: Description               | `00000000-0000-0000-0000-000000003114` |  
| `collibra.assets.dataProduct.containsOutputPortRelationId` | ID of the relation type that links Data Products and Output Ports. Default: Data Asset groups Data Asset   | `00000000-0000-0000-0000-000000007017` |  
| `collibra.assets.outputPort.typeId`                        | ID of the asset that represents Output Ports. Default: Data Set                                            | `00000000-0000-0000-0001-000400000001` |
| `collibra.assets.outputPort.attributes.description`        | ID that represents the attribute type for the Output Port description. Default: Description                | `00000000-0000-0000-0000-000000003114` |  
| `collibra.assets.outputPort.attributes.tableType`          | ID that represents the attribute type for the Output Port type. Default: Table Type                        | `00000000-0000-0000-0001-000500000008` |  
| `collibra.assets.outputPort.containsColumnRelationId`      | ID of the relation type that links Output Ports and their columns. Default: Data Set contains Data Element | `00000000-0000-0000-0000-000000007062` |  
| `collibra.assets.column.typeId`                            | ID of the asset that represents Columns. Default: Column                                                   | `00000000-0000-0000-0000-000000031008` |
| `collibra.assets.column.attributes.description`            | ID that represents the attribute type for the Column description. Default: Description                     | `00000000-0000-0000-0000-000000003114` |  
| `collibra.assets.column.attributes.dataType`               | ID that represents the data type of a column. Default: Technical Data Type                                 | `00000000-0000-0000-0000-000000000219` |  
| `collibra.assets.businessTerm.typeId`                      | ID of the asset that represents business terms. Default: Business Term                                     | `00000000-0000-0000-0000-000000011001` |

### Custom URL Picker configuration

This microservice provides support for Custom URL Picker endpoints, in order to retrieve a set of business terms based on a configurable asset type ID (see [above](#mapping-between-witboost-and-collibra)), and an input with filters based on text and an optional domain set on the appropriate Witboost template. In order for these endpoints to be reachable by Witboost user interface, we need to allow CORS on the set of the picker endpoints. For this, we provide two configurations in order to set the endpoint mask and the origin URL from which requests will be allowed.

| Configuration                         | Description                         | Default            |
|:--------------------------------------|:------------------------------------|:-------------------|
| `custom-url-picker.cors.endpointMask` | Endpoint mask to allow CORS         | `/v1/resources/**` |
| `custom-url-picker.cors.origin`       | Origin URL allowed on CORS requests |                    |
