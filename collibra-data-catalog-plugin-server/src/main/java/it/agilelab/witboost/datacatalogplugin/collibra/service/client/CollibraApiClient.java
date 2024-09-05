package it.agilelab.witboost.datacatalogplugin.collibra.service.client;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import it.agilelab.witboost.datacatalogplugin.collibra.client.api.*;
import it.agilelab.witboost.datacatalogplugin.collibra.client.invoker.ApiClient;
import it.agilelab.witboost.datacatalogplugin.collibra.client.model.*;
import it.agilelab.witboost.datacatalogplugin.collibra.config.CollibraAPIConfig;
import it.agilelab.witboost.datacatalogplugin.collibra.config.CollibraConfig;
import it.agilelab.witboost.datacatalogplugin.collibra.model.witboost.*;
import java.io.IOException;
import java.math.BigInteger;
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
    private final AssetsApi assetsApiClient;
    private final AttributesApi attributesApiClient;
    private final RelationsApi relationsApiClient;
    private final CommunitiesApi communitiesApiClient;
    private final DomainsApi domainsApiClient;

    private final AuthenticationSessionsApi authenticationSessionsApi;

    private final DomainCollibraApiClient domainCollibraApiClient;
    private final DataProductCollibraApiClient dataProductCollibraApiClient;
    private final BusinessTermCollibraApiClient businessTermCollibraApiClient;

    private String token = "invalid";
    private final String TOKEN_HEADER = "X-CSRF-TOKEN";
    private final String COOKIE_NAME = "JSESSIONID";

    private final Logger logger = LoggerFactory.getLogger(CollibraApiClient.class);

    public CollibraApiClient(CollibraConfig collibraConfig, CollibraAPIConfig collibraAPIConfig) {
        this.collibraAPIConfig = collibraAPIConfig;
        this.collibraConfig = collibraConfig;

        this.apiClient = new ApiClient(buildRestTemplate());
        var trimmedEndpoint = collibraAPIConfig.endpoint().endsWith("/")
                ? collibraAPIConfig
                        .endpoint()
                        .substring(0, collibraAPIConfig.endpoint().length() - 1)
                : collibraAPIConfig.endpoint();
        this.apiClient.setBasePath(trimmedEndpoint + "/rest/2.0");
        this.apiClient.setUsername(collibraAPIConfig.username());
        this.apiClient.setPassword(collibraAPIConfig.password());

        this.authenticationSessionsApi = new AuthenticationSessionsApi(apiClient);
        this.assetsApiClient = new AssetsApi(apiClient);
        this.attributesApiClient = new AttributesApi(apiClient);
        this.relationsApiClient = new RelationsApi(apiClient);
        this.communitiesApiClient = new CommunitiesApi(apiClient);
        this.domainsApiClient = new DomainsApi(apiClient);

        this.domainCollibraApiClient =
                new DomainCollibraApiClient(collibraConfig, communitiesApiClient, domainsApiClient);
        this.dataProductCollibraApiClient = new DataProductCollibraApiClient(
                collibraConfig, assetsApiClient, attributesApiClient, relationsApiClient, domainCollibraApiClient);
        this.businessTermCollibraApiClient = new BusinessTermCollibraApiClient(collibraConfig, assetsApiClient);
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
        return dataProductCollibraApiClient.createDataProduct(dataProduct, domainId);
    }

    public Asset updateDataProduct(UUID id, DataProduct dataProduct, UUID domainId) {
        return dataProductCollibraApiClient.updateDataProduct(id, dataProduct, domainId);
    }

    public void deleteDataProduct(DataProduct dataProduct) {
        dataProductCollibraApiClient.removeDataProduct(dataProduct);
    }

    public Optional<Asset> findAssetForDataProduct(DataProduct dataProduct) {
        return dataProductCollibraApiClient.findAssetForDataProduct(dataProduct);
    }

    public Asset upsertDataProduct(DataProduct dataProduct) {
        return dataProductCollibraApiClient.upsertDataProduct(dataProduct);
    }

    public Optional<DomainGroup> findDomainByName(String name) {
        return domainCollibraApiClient.findDomainByName(name);
    }

    public DomainGroup upsertDomain(String name) {
        return domainCollibraApiClient.upsertDomain(name);
    }

    public List<Asset> findBusinessTermAssets(
            BigInteger offset, BigInteger limit, Option<String> nameFilter, Optional<DomainGroup> domain) {
        return businessTermCollibraApiClient.findBusinessTermAssets(offset, limit, nameFilter, domain);
    }

    public Option<Asset> findBusinessTermAsset(String id) {
        return businessTermCollibraApiClient.findBusinessTermAsset(id);
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
