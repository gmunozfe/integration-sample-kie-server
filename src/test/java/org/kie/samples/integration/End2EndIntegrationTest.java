package org.kie.samples.integration;

import static java.util.Collections.singletonMap;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.rules.ExpectedException;
import org.kie.samples.integration.testcontainers.KieServerContainer;
import org.kie.samples.integration.testcontainers.LdapContainer;
import org.kie.server.api.exception.KieServicesHttpException;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.ProcessServicesClient;
import org.kie.server.client.QueryServicesClient;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.github.dockerjava.api.DockerClient;

@Testcontainers(disabledWithoutDocker=true)
public class End2EndIntegrationTest {
    
    public static final String ARTIFACT_ID = "ldap-sample";
    public static final String GROUP_ID = "org.kie.server.testing";
    public static final String VERSION = "1.0.0";
    public static final String ALIAS = "-alias";
    
    public static final String USER_WITH_ADMIN_ROLES = "krisv";
    public static final String USER_NOT_EXIST = "notExistUser";
    public static final String EMPTY_USER = "";
    public static final String CORRECT_PASSWORD = "krisv3";
    public static final String WRONG_USER = "fake_user";
    public static final String WRONG_PASSWORD = "krisv";
    public static final String USER_WITH_NO_ROLES = "userWithNoRoles";
    public static final String PW_FOR_USER_WITH_NO_ROLES = "pwWithNoRoles";
    public static final String EMPTY_PASSWORD = "";
    
    public static String containerId = GROUP_ID+":"+ARTIFACT_ID+":"+VERSION;

    private static Logger logger = LoggerFactory.getLogger(End2EndIntegrationTest.class);
    
    private static Map<String, String> args = new HashMap<>();

    static {
        args.put("IMAGE_NAME", System.getProperty("org.kie.samples.image"));
        args.put("START_SCRIPT", System.getProperty("org.kie.samples.script"));
        args.put("SERVER", System.getProperty("org.kie.samples.server"));
    }
    
    @ClassRule
    public static Network network = Network.newNetwork();
    
    @ClassRule
    public static LdapContainer ldap = new LdapContainer(network);

    @ClassRule
    public static KieServerContainer kieServer = new KieServerContainer(network, args);
    
    @ClassRule
    public static BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>()
            .withNetwork(network)
            .withNetworkAliases("vnchost")
            .withCapabilities(new ChromeOptions())
            .withRecordingMode(RECORD_ALL, new File("target"));
    
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    
    @BeforeClass
    public static void setup() {
        logger.info("KIE SERVER started at port "+kieServer.getKiePort());
        logger.info("LDAP started at port "+ldap.getLdapPort());
    }
    
	@AfterClass
    public static void tearDown() throws Exception {
     DockerClient docker = DockerClientFactory.instance().client();
     docker.listImagesCmd().withLabelFilter("autodelete=true").exec().stream()
     .filter(c -> c.getId() != null)
     .forEach(c -> docker.removeImageCmd(c.getId()).withForce(true).exec());
    }
    
    
    @Test
    public void whenWrongPasswordThenReturnUnauthorized() throws Exception {
        expectedError("Error code: 401");
        authenticate(USER_WITH_ADMIN_ROLES, WRONG_PASSWORD);
    }
    
    @Test
    public void whenWrongUserThenReturnUnauthorized() throws Exception {
    	expectedError("Error code: 401");
        authenticate(WRONG_USER, WRONG_PASSWORD);
    }
   
    @Test
    public void whenNoRolesUserThenReturnForbidden() throws Exception {
    	expectedError("Error code: 403");
        authenticate(USER_WITH_NO_ROLES, PW_FOR_USER_WITH_NO_ROLES);
    }
    
    @Test
    @DisplayName("when user logged in has guardRole then restricted var can be changed")
    public void whenUserLoggedInHasGuardRoleThenRestrictedVarCanBeChanged() throws Exception {
    	KieServicesClient ksClient = authenticate("Bartlet", "123456");
    	
        createContainer(ksClient);
        ProcessServicesClient processClient = ksClient.getServicesClient(ProcessServicesClient.class);
        // authorized user can start process instance and update the restricted variable
        Long processInstanceId = processClient.startProcess(containerId, "HumanTaskWithRestrictedVar", singletonMap("press", "true"));
        assertNotNull(processInstanceId);
       
        abortProcess(ksClient, processClient, processInstanceId);
        ksClient.disposeContainer(containerId);
    }
    
    @Test
    @DisplayName("when user logged in hasn't guardRole then restricted var cannot be changed")
    public void whenUserLoggedInHasnotGuardRoleThenRestrictedVarCannotBeChanged() throws Exception {
    	KieServicesClient ksClient = authenticate("krisv", "krisv3");
        
        createContainer(ksClient);
        ProcessServicesClient processClient = ksClient.getServicesClient(ProcessServicesClient.class);
        
        exceptionRule.expectMessage("violated");
        Long processInstanceId = processClient.startProcess(containerId, "HumanTaskWithRestrictedVar", singletonMap("press", "true"));
        assertNull(processInstanceId);
        
        ksClient.disposeContainer(containerId);
    }
    
    @Test
    @DisplayName("when user logged in hasn't guardRole then can start process without changing restricted var")
    public void whenUserLoggedInHasNotGuardRoleThenCanStartProcess() throws Exception {
    	KieServicesClient ksClient = authenticate("krisv", "krisv3");
    	
        createContainer(ksClient);
        ProcessServicesClient processClient = ksClient.getServicesClient(ProcessServicesClient.class);
        // authorized user can start process instance and update the restricted variable
        Long processInstanceId = processClient.startProcess(containerId, "HumanTaskWithRestrictedVar");
        assertNotNull(processInstanceId);
       
        abortProcess(ksClient, processClient, processInstanceId);
        ksClient.disposeContainer(containerId);
    }
    
    /*@Test
    public void loginSucessfulWebUI() throws InterruptedException {
        RemoteWebDriver webDriver = loginUI(USER_WITH_ADMIN_ROLES, CORRECT_PASSWORD);

        boolean loaded = new WebDriverWait(webDriver, 90)
           .until(ExpectedConditions.urlContains("org.kie.workbench.common.screens.home.client.HomePresenter"));
        
        assertTrue(loaded);
    }*/

    private RemoteWebDriver loginUI(String user, String password) {
        RemoteWebDriver webDriver = chrome.getWebDriver();

        webDriver.get("http://kie-server:8080/business-central");

        webDriver.findElementByName("j_username").sendKeys(user);
        webDriver.findElementByName("j_password").sendKeys(password);
        webDriver.findElementByXPath("//input[@type='submit']").click();
        return webDriver;
    }
    
    private void expectedError(String message) {
        exceptionRule.expectCause(allOf(isA(KieServicesHttpException.class), hasProperty("message", containsString(message))));
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
    
    private void abortProcess(KieServicesClient kieServicesClient, ProcessServicesClient processClient, Long processInstanceId) {
        QueryServicesClient queryClient = kieServicesClient.getServicesClient(QueryServicesClient.class);
        
        ProcessInstance processInstance = queryClient.findProcessInstanceById(processInstanceId);
        assertNotNull(processInstance);
        assertEquals(1, processInstance.getState().intValue());
        processClient.abortProcessInstance(containerId, processInstanceId);
    }
}

