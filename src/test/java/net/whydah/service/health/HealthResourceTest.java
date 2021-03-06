package net.whydah.service.health;

import net.whydah.testsupport.AbstractEndpointTest;
import org.testng.annotations.Test;

import java.net.HttpURLConnection;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class HealthResourceTest extends AbstractEndpointTest {

    @Test
    public void testHealth() {
        given()
                .when()
                .port(getServerPort())
                .get(HealthResource.HEALTH_PATH)
                .then().log().ifValidationFails()
                .statusCode(HttpURLConnection.HTTP_OK)
                .body("hasApplicationToken", equalTo("true"))
                .body("hasValidApplicationToken", equalTo("true"))
                .body("Status", equalTo("OK"));
    }
}
