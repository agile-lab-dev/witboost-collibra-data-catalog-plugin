package it.agilelab.witboost.datacatalogplugin.collibra.service;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import it.agilelab.witboost.datacatalogplugin.collibra.client.api.*;
import it.agilelab.witboost.datacatalogplugin.collibra.client.invoker.ApiClient;
import it.agilelab.witboost.datacatalogplugin.collibra.client.model.*;
import it.agilelab.witboost.datacatalogplugin.collibra.common.CollibraAPIConfig;
import it.agilelab.witboost.datacatalogplugin.collibra.common.CollibraConfig;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.Column;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.DataProduct;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.OutputPort;
import it.agilelab.witboost.datacatalogplugin.collibra.parser.Parser;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Service
public class CollibraApiClient {

    private final CollibraAPIConfig collibraAPIConfig;
    private final CollibraConfig collibraConfig;
    private final ApiClient apiClient;
    private final AuthenticationSessionsApi authenticationSessionsApi;
    private final AssetsApi assetsApiClient;
    private final DomainsApi domainsApiClient;
    private final AttributesApi attributesApiClient;
    private final RelationsApi relationsApiClient;
    private String token = "invalid";
    private final String TOKEN_HEADER = "X-CSRF-TOKEN";
    private final String COOKIE_NAME = "JSESSIONID";
    private static final String OUTPUTPORT_KIND = "outputport";

    private final Logger logger = LoggerFactory.getLogger(CollibraApiClient.class);

    public CollibraApiClient(CollibraConfig collibraConfig, CollibraAPIConfig collibraAPIConfig) {
        this.collibraAPIConfig = collibraAPIConfig;
        this.collibraConfig = collibraConfig;

        this.apiClient = new ApiClient(buildRestTemplate());
        this.apiClient.setBasePath(collibraAPIConfig.basePath() + "/rest/2.0");
        this.apiClient.setUsername(collibraAPIConfig.username());
        this.apiClient.setPassword(collibraAPIConfig.password());

        this.authenticationSessionsApi = new AuthenticationSessionsApi(apiClient);
        this.assetsApiClient = new AssetsApi(apiClient);
        this.domainsApiClient = new DomainsApi(apiClient);
        this.attributesApiClient = new AttributesApi(apiClient);
        this.relationsApiClient = new RelationsApi(apiClient);
    }

    @Deprecated
    public Tuple2<String, List<Tuple2<String, String>>> login() {
        var loginRequest =
                new LoginRequest().username(collibraAPIConfig.username()).password(collibraAPIConfig.password());
        var loginResponse = authenticationSessionsApi.loginWithHttpInfo(loginRequest);

        if (!loginResponse.getStatusCode().is2xxSuccessful()) {
            var msg = "Login failed, response: " + loginResponse;
            logger.error(msg);
            throw new RuntimeException(msg);
        } else {
            logger.info("Login successful");
        }

        // can't get token from the body as the generated client ignores it; grab it from the cookie
        var headers = loginResponse.getHeaders();
        var jsessionidCookie = headers.get("Set-Cookie").stream()
                .filter(s -> s.startsWith("JSESSIONID"))
                .findFirst()
                .get();
        token = jsessionidCookie.split(";")[0].split("=")[1];

        // set header
        logger.debug("Setting default header: " + TOKEN_HEADER);
        apiClient.addDefaultHeader(TOKEN_HEADER, token);

        // get all cookies and set them as defaults
        List<Tuple2<String, String>> cookies = headers.get("Set-Cookie").stream()
                .map(setCookie -> {
                    // TODO: should not ignore rest of the header (secure, path etc)
                    var cookieString = setCookie.split(";")[0];
                    return splitCookieString(cookieString);
                })
                .toList();
        logger.debug(
                "Setting default cookies: " + cookies.stream().map(Tuple2::_1).collect(Collectors.joining(", ")));
        System.out.println(
                "Full cookie details: " + cookies.stream().map(Tuple2::toString).collect(Collectors.joining(", ")));
        cookies.forEach(cookie -> apiClient.addDefaultCookie(cookie._1, cookie._2));

        return Tuple.of(token, cookies);
    }

    private static Tuple2<String, String> splitCookieString(String cookieString) {
        int indexOfFirstEqual = cookieString.indexOf('=');

        String cookieName = cookieString.substring(0, indexOfFirstEqual);
        String cookieValue = cookieString.substring(indexOfFirstEqual + 1);

        return Tuple.of(cookieName, cookieValue);
    }

    @Deprecated
    public boolean isLoggedIn() {
        // resttemplate throws exceptions on all 4xx errors, need to catch (or change the handler)
        try {
            var loginStatus = authenticationSessionsApi.isLoggedInWithHttpInfo();
            return loginStatus.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException e) {
            return e.getStatusCode().is2xxSuccessful();
        }
    }

    public Asset createDataProduct(DataProduct dataProduct, UUID domainId) {

        var addAssetRequest = new AddAssetRequest();

        addAssetRequest
                .name(dataProduct.getId())
                .displayName(dataProduct.getName())
                .statusId(UUID.fromString(collibraConfig.statusId()))
                .domainId(domainId)
                .typeId(UUID.fromString(collibraConfig.dataProductTypeId()))
                .excludedFromAutoHyperlinking(true);

        var asset = assetsApiClient.addAsset(addAssetRequest);

        updateDataProductAttributes(asset.getId(), dataProduct);

        var outputPorts = extractOutputPorts(dataProduct);
        updateOutputPorts(domainId, asset.getId(), outputPorts);

        return asset;
    }

    public Asset updateDataProduct(UUID id, DataProduct dataProduct, UUID domainId) {
        var changeAssetRequest = new ChangeAssetRequest();

        changeAssetRequest
                .name(dataProduct.getId())
                .displayName(dataProduct.getName())
                .statusId(UUID.fromString(collibraConfig.statusId()))
                .domainId(domainId)
                .typeId(UUID.fromString(collibraConfig.dataProductTypeId()))
                .excludedFromAutoHyperlinking(true);

        var asset = assetsApiClient.changeAsset(id, changeAssetRequest);

        updateDataProductAttributes(asset.getId(), dataProduct);

        var outputPorts = extractOutputPorts(dataProduct);
        updateOutputPorts(domainId, asset.getId(), outputPorts);

        return asset;
    }

    private void updateDataProductAttributes(UUID id, DataProduct dataProduct) {
        var attributes = findAttributesForAsset(id);
        var attributeIds = attributes.stream().map(Attribute::getId).collect(Collectors.toList());
        attributesApiClient.removeAttributes(attributeIds);

        var descriptionAttributeTypeId = UUID.fromString(collibraConfig.descriptionAttributeTypeId());
        List<Tuple2<UUID, String>> newAttributes =
                List.of(Tuple.of(descriptionAttributeTypeId, dataProduct.getDescription()));

        for (Tuple2<UUID, String> newAttribute : newAttributes) {
            var addAttributeRequest = new AddAttributeRequest()
                    .assetId(id)
                    .typeId(newAttribute._1)
                    .value(newAttribute._2);
            attributesApiClient.addAttribute(addAttributeRequest);
        }
    }

    private List<Attribute> findAttributesForAsset(UUID id) {
        var attributes = attributesApiClient.findAttributes(0, 1000, null, id, null, null);
        return attributes.getResults();
    }

    // TODO make private
    public List<OutputPort> extractOutputPorts(DataProduct dataProduct) {
        return dataProduct.getComponents().stream()
                .filter(component -> component.get("kind").asText("none").equals(OUTPUTPORT_KIND))
                .map(outputport -> Parser.parseComponent(outputport, OutputPort.class))
                .filter(Either::isRight)
                .map(Either::get)
                .map(x -> (OutputPort) x)
                .toList();
    }

    private void updateOutputPorts(UUID domainId, UUID dataProductId, List<OutputPort> outputPorts) {
        var relations = relationsApiClient
                .findRelations(0, 1000, null, dataProductId, null, null)
                .getResults();

        for (Relation relation : relations) {
            var targetId = relation.getTarget().getId();
            relationsApiClient.removeRelation1(relation.getId());
            assetsApiClient.removeAsset(targetId);
        }

        // TODO support multiple output ports
        var maybeOutputPort = outputPorts.stream().findFirst();
        if (outputPorts.size() > 1) logger.warn("Ignoring Output Ports after the first one");

        if (maybeOutputPort.isPresent()) {
            var outputPort = maybeOutputPort.get();
            logger.info("Adding Output Port: " + outputPort);

            var columns = outputPort.getDataContract().getSchema();

            for (Column column : columns) {
                var addAssetRequest = new AddAssetRequest();

                addAssetRequest
                        .domainId(domainId)
                        .name(outputPort.getId() + ":" + column.getName())
                        .displayName(outputPort.getName() + " > " + column.getName())
                        .typeId(UUID.fromString(collibraConfig.columnTypeId()))
                        .excludedFromAutoHyperlinking(true);

                logger.info("Adding asset: " + addAssetRequest);
                var asset = assetsApiClient.addAsset(addAssetRequest);
                logger.info("Asset added: " + asset);

                var addRelationRequest = new AddRelationRequest();
                addRelationRequest
                        .sourceId(dataProductId)
                        .targetId(asset.getId())
                        .typeId(UUID.fromString(collibraConfig.relationTypeId()));

                logger.info("Adding relation: " + addRelationRequest);
                var relation = relationsApiClient.addRelation(addRelationRequest);
                logger.info("Relation added: " + relation);
            }
        } else {
            logger.warn("Data Product has no valid Output Ports");
        }
    }

    public Optional<Asset> findAssetForDataProduct(DataProduct dataProduct) {
        var response = assetsApiClient.findAssets(
                0, 10, dataProduct.getId(), "EXACT", null, null, null, null, null, null, null, null, null);

        return response.getResults().stream().findFirst();
    }

    public Asset upsertDataProduct(DataProduct dataProduct) {
        var domain = upsertDomain(dataProduct.getDomain());

        var maybeAsset = findAssetForDataProduct(dataProduct);

        if (maybeAsset.isEmpty()) {
            return createDataProduct(dataProduct, domain.getId());
        } else {
            var id = maybeAsset.get().getId();
            return updateDataProduct(id, dataProduct, domain.getId());
        }
    }

    public Optional<Domain> findDomainByName(String name) {
        var response = domainsApiClient.findDomains(0, 10, name, "EXACT", null, null, null, null);

        return response.getResults().stream().findFirst();
    }

    public Domain upsertDomain(String name) {
        var maybeDomain = findDomainByName(name);

        if (maybeDomain.isEmpty()) {
            return createDomain(name);
        } else {
            var id = maybeDomain.get().getId();
            return updateDomain(id, name);
        }
    }

    public Domain createDomain(String name) {
        var addDomainRequest = new AddDomainRequest()
                .name(name)
                .typeId(UUID.fromString(collibraConfig.domainTypeId()))
                .communityId(UUID.fromString(collibraConfig.communityId()))
                .excludedFromAutoHyperlinking(true);

        return domainsApiClient.addDomain(addDomainRequest);
    }

    public Domain updateDomain(UUID id, String name) {
        var changeDomainRequest = new ChangeDomainRequest().name(name).excludedFromAutoHyperlinking(true);

        return domainsApiClient.changeDomain(id, changeDomainRequest);
    }

    private RestTemplate buildRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // this allows us to read the response more than once - Necessary for debugging.
        restTemplate.setRequestFactory(new BufferingClientHttpRequestFactory(restTemplate.getRequestFactory()));

        // disable default URL encoding
        DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory();
        uriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);
        restTemplate.setUriTemplateHandler(uriBuilderFactory);

        // add autologin interceptor and patch interceptor
        // restTemplate.setInterceptors(List.of(new LoginInterceptor(), new PatchInterceptor()));
        restTemplate.setInterceptors(List.of(new PatchInterceptor()));

        return restTemplate;
    }

    private class LoginInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
                throws IOException {

            if (!isLoginRequest(request)) {
                if (!isLoggedIn()) {
                    logger.debug("Not logged in, attempting login");

                    var tokenAndCookies = login();
                    var token = tokenAndCookies._1;
                    var cookies = tokenAndCookies._2;

                    // update request headers/cookie
                    var headers = request.getHeaders();
                    headers.remove(TOKEN_HEADER);
                    logger.debug("Setting header for request: " + TOKEN_HEADER);
                    headers.add(TOKEN_HEADER, token);
                    // TODO keep additional cookies and only replace new ones
                    var cookiesString = cookies.stream()
                            .map(cookie -> cookie._1 + "=" + cookie._2)
                            .collect(Collectors.joining("; "));
                    headers.remove("Cookie");
                    logger.debug("Setting cookies for request: "
                            + cookies.stream().map(Tuple2::_1).collect(Collectors.joining(", ")));
                    System.out.println("Full cookie details: " + cookiesString);
                    headers.add("Cookie", cookiesString);
                } else {
                    logger.debug("Already logged in, proceeding");
                }
            } else {
                logger.debug("Skipping interception of login request");
            }

            return execution.execute(request, body);
        }

        private boolean isLoginRequest(HttpRequest request) {
            var requestUri = request.getURI();
            var requestMethod = request.getMethod();
            var isLoginCheck =
                    requestUri.toString().endsWith("/auth/sessions/current") && requestMethod == HttpMethod.GET;
            var isLoginAttempt = requestUri.toString().endsWith("/auth/sessions") && requestMethod == HttpMethod.POST;
            return isLoginCheck || isLoginAttempt;
        }
    }

    // change PATCH requests to POST + override headers, as HttpURLConnection does not support PATCH
    // https://bugs.openjdk.org/browse/JDK-7016595
    private class PatchInterceptor implements ClientHttpRequestInterceptor {

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
                throws IOException {
            if (request.getMethod() == HttpMethod.PATCH) {
                HttpRequest wrapper = new HttpRequestWrapper(request) {
                    @Override
                    public HttpMethod getMethod() {
                        return HttpMethod.POST;
                    }
                };

                wrapper.getHeaders().set("X-HTTP-Method-Override", "PATCH");

                return execution.execute(wrapper, body);
            } else {
                return execution.execute(request, body);
            }
        }
    }
}
