package net.whydah.service.auth;

import com.jayway.restassured.response.ValidatableResponse;
import net.whydah.demoservice.testsupport.AbstractEndpointTest;
import net.whydah.util.Configuration;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static org.testng.Assert.*;

/**
 *
 */
public class SSOLoginResourceTest extends AbstractEndpointTest {

    @Test
    public void whenInitializeUserLogin_appNameOrSecret_isNotFound_404Returned() {
        String apiPath = "/appThatDoesNotExist/user/auth/ssologin/";
        given()
                .when()
                .port(getServerPort())
                .post(apiPath)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void verifyInitializeUserLoginWithAppName() {
        String apiPath = "/testApp/user/auth/ssologin/";
        ValidatableResponse response = given()
                .when()
                .port(getServerPort())
                .post(apiPath)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.OK.getStatusCode());
        String ssoLoginUrl = response.extract().path("ssoLoginUrl");
        String ssoLoginUUID = response.extract().path("ssoLoginUUID");
        assertEquals(ssoLoginUrl, getBaseUrl() + apiPath + ssoLoginUUID);

        // Throws exception if it does not conform with UUID format
        UUID uuid = UUID.fromString(ssoLoginUUID);
        assertNotNull(uuid);

    }

    @Test
    public void verifyInitializeUserLoginWithSecret() {
        // Extract secret from a load for the application
        ValidatableResponse validatableResponse = given()
                .when()
                .port(getServerPort())
                .redirects().follow(false) //Do not follow the redirect
                .get("/load/testApp")
                .then().log().ifError()
                .statusCode(Response.Status.FOUND.getStatusCode());

        String locationHeader = validatableResponse.extract().header("Location");
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUriString(locationHeader).build().getQueryParams();
        String secret = queryParams.get("code").get(0);
        assertNotNull(secret);
        assertFalse(secret.isEmpty());


        // Initialize the user login
        String apiPath = "/" + secret + "/user/auth/ssologin/";
        ValidatableResponse response = given()
                .when()
                .port(getServerPort())
                .post(apiPath)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.OK.getStatusCode());
        String ssoLoginUrl = response.extract().path("ssoLoginUrl");
        String ssoLoginUUID = response.extract().path("ssoLoginUUID");
        assertEquals(ssoLoginUrl, getBaseUrl() + apiPath + ssoLoginUUID);

        // Throws exception if it does not conform with UUID format
        UUID uuid = UUID.fromString(ssoLoginUUID);
        assertNotNull(uuid);
    }


    @Test
    public void whenRedirectUserLogin_appNameOrSecret_isNotFound_404Returned() {
        String apiPath = "/appThatDoesNotExist/user/auth/ssologin/" + UUID.randomUUID().toString();
        given()
                .when()
                .port(getServerPort())
                .get(apiPath)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void whenRedirectUserLogin_ssoLoginUUID_isNotFound_404Returned() {
        String apiPath = "/appThatDoesNotExist/user/auth/ssologin/" + UUID.randomUUID().toString();
        given()
                .when()
                .port(getServerPort())
                .get(apiPath)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void verifyRedirectInitializedUserLoginWithAppName() {
        String apiPath = "/testApp/user/auth/ssologin/";
        ValidatableResponse initResponse = given()
                .when()
                .port(getServerPort())
                .post(apiPath)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.OK.getStatusCode());
        String ssoLoginUrl = initResponse.extract().path("ssoLoginUrl");

        ValidatableResponse redirectResponse = given()
                .when()
                .redirects().follow(false) //Do not follow the redirect
                .get(ssoLoginUrl)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.FOUND.getStatusCode());

        String location = redirectResponse.extract().header("Location");

        assertNotNull(location);
        assertFalse(location.isEmpty());

        String expectedRedirectURI = Configuration.getString("myuri") + "/load/testApp";

        String expectedLocation = UriBuilder.fromUri(Configuration.getString("logonservice"))
                .path("login")
                .queryParam("redirectURI", expectedRedirectURI)
                .queryParam("appName", "testApp")
                .build()
                .toString();

        assertEquals(location, expectedLocation);
    }

    @Test
    public void whenRedirectUserLogin_UserCheckoutIsForwarded_inLocation() {
        String apiPath = "/testApp/user/auth/ssologin/";
        ValidatableResponse initResponse = given()
                .when()
                .port(getServerPort())
                .post(apiPath)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.OK.getStatusCode());
        String ssoLoginUrl = initResponse.extract().path("ssoLoginUrl");

        ValidatableResponse redirectResponse = given()
                .when()
                .redirects().follow(false) //Do not follow the redirect
                .get(ssoLoginUrl + "?UserCheckout=true") //Add UserCheckout query param
                .then().log().ifValidationFails()
                .statusCode(Response.Status.FOUND.getStatusCode());

        String location = redirectResponse.extract().header("Location");

        assertNotNull(location);
        assertFalse(location.isEmpty());

        String expectedRedirectURI = Configuration.getString("myuri") + "/load/testApp";

        String expectedLocation = UriBuilder.fromUri(Configuration.getString("logonservice"))
                .path("login")
                .queryParam("redirectURI", expectedRedirectURI)
                .queryParam("UserCheckout", "true")
                .queryParam("appName", "testApp")
                .build()
                .toString();

        assertEquals(location, expectedLocation);
    }

    @Test
    public void verifyRedirectInitializedUserLoginWithSecret() throws UnsupportedEncodingException {
        final String testAppName = "testApp";

        // Extract secret from a load for the application
        ValidatableResponse validatableResponse = given()
                .when()
                .port(getServerPort())
                .redirects().follow(false) //Do not follow the redirect
                .get("/load/" + testAppName)
                .then().log().ifError()
                .statusCode(Response.Status.FOUND.getStatusCode());

        String locationHeader = validatableResponse.extract().header("Location");
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUriString(locationHeader).build().getQueryParams();
        String secret = queryParams.get("code").get(0);
        assertNotNull(secret);
        assertFalse(secret.isEmpty());


        // Initialize the user login
        String apiPath = "/" + secret + "/user/auth/ssologin/";
        ValidatableResponse initResponse = given()
                .when()
                .port(getServerPort())
                .post(apiPath)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.OK.getStatusCode());
        String ssoLoginUrl = initResponse.extract().path("ssoLoginUrl");

        ValidatableResponse redirectResponse = given()
                .when()
                .redirects().follow(false) //Do not follow the redirect
                .get(ssoLoginUrl)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.FOUND.getStatusCode());

        String location = redirectResponse.extract().header("Location");

        assertNotNull(location);
        assertFalse(location.isEmpty());

        String expectedRedirectURI = Configuration.getString("myuri") + "/load/" + testAppName;

        String expectedLocation = UriBuilder.fromUri(Configuration.getString("logonservice"))
                .path("login")
                .queryParam("redirectURI", expectedRedirectURI)
                .queryParam("appName", testAppName)
                .build()
                .toString();

        assertEquals(location, expectedLocation);
    }


}