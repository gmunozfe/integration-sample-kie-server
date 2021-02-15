package org.kie.samples.integration;


import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.kie.samples.integration.testcontainers.KieServerContainer;
import org.kie.samples.integration.testcontainers.LdapContainer;
import org.kie.server.api.exception.KieServicesHttpException;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.allOf;

@Testcontainers(disabledWithoutDocker=true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class KieIntegrationTest {
    
    private static Logger logger = LoggerFactory.getLogger(KieIntegrationTest.class);

    public static Network network = Network.newNetwork();
    
    private static final String ARTIFACT_ID = "ldap-sample";
    private static final String GROUP_ID = "org.kie.server.testing";
    private static final String VERSION = "1.0.0";
    private static final String ALIAS = "-alias";
    
    private static final String USER_WITH_ADMIN_ROLES = "krisv";
    private static final String USER_NOT_EXIST = "notExistUser";
    private static final String EMPTY_USER = "";
    private static final String CORRECT_PASSWORD = "krisv3";
    private static final String WRONG_USER = "fake_user";
    private static final String WRONG_PASSWORD = "krisv";
    private static final String USER_WITH_NO_ROLES = "userWithNoRoles";
    private static final String PW_FOR_USER_WITH_NO_ROLES = "pwWithNoRoles";
    private static final String EMPTY_PASSWORD = "";
    
    private String containerId = GROUP_ID+":"+ARTIFACT_ID+":"+VERSION;

    
    @ClassRule
    public static LdapContainer ldap = new LdapContainer(network);

    @ClassRule
    public static KieServerContainer kieServer = new KieServerContainer(network);
    
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @BeforeClass
    public static void setup() {
        logger.info("KIE SERVER started at port "+kieServer.getKiePort());
        logger.info("LDAP started at port "+ldap.getLdapPort());
    }

    @Test
    public void whenWrongPasswordThenReturnUnauthorized() throws Exception {
        expectedErrorCode(401);
        authenticate(USER_WITH_ADMIN_ROLES, WRONG_PASSWORD);
    }
    
    @Test
    public void whenWrongUserThenReturnUnauthorized() throws Exception {
        expectedErrorCode(401);
        authenticate(WRONG_USER, WRONG_PASSWORD);
    }
    
    @Test
    public void whenNoRolesUserThenReturnForbidden() throws Exception {
        expectedErrorCode(403);
        authenticate(USER_WITH_NO_ROLES, PW_FOR_USER_WITH_NO_ROLES);
    }
    
    @Test
    public void whenCorrectUserPasswordThenContainerCanBeCreated() throws Exception {
        KieServicesClient client = authenticate(USER_WITH_ADMIN_ROLES, CORRECT_PASSWORD);
        createContainer(client);
    }
    
    private void expectedErrorCode(int errorCode) {
        exceptionRule.expectCause(allOf(isA(KieServicesHttpException.class), hasProperty("message", containsString("Error code: "+errorCode))));
    }
    
    private void createContainer(KieServicesClient client) {
        ReleaseId releaseId = new ReleaseId(GROUP_ID, ARTIFACT_ID, VERSION);
        KieContainerResource resource = new KieContainerResource(containerId, releaseId);
        resource.setContainerAlias(ARTIFACT_ID + ALIAS);
        client.createContainer(containerId, resource);
    }

    private KieServicesClient authenticate(String user, String password) {
        String serverUrl = "http://localhost:" + kieServer.getKiePort() + "/kie-server/services/rest/server";
        KieServicesConfiguration configuration = KieServicesFactory.newRestConfiguration(serverUrl, user, password);
        
        configuration.setTimeout(60000);
        configuration.setMarshallingFormat(MarshallingFormat.JSON);
        return  KieServicesFactory.newKieServicesClient(configuration);
    }
    
}

