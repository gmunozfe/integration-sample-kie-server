package org.kie.samples.integration;


import java.io.File;

import org.appformer.maven.integration.MavenRepository;
import org.junit.ClassRule;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.samples.integration.testcontainers.KieServerContainer;
import org.kie.samples.integration.testcontainers.LdapContainer;
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

import static org.appformer.maven.integration.MavenRepository.getMavenRepository;

@Testcontainers(disabledWithoutDocker=true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class KieIntegrationTest {
    
	private static Logger logger = LoggerFactory.getLogger(KieIntegrationTest.class);

    public static Network network = Network.newNetwork();
    
    private static final String ARTIFACT_ID = "ldap-sample";
    private static final String GROUP_ID = "org.kie.server.testing";
    private static final String VERSION = "1.0.0";
    
    private String containerId = GROUP_ID+":"+ARTIFACT_ID+":"+VERSION;

    
    @ClassRule
    public static LdapContainer ldap = new LdapContainer(network);

    @ClassRule
    public static KieServerContainer kieServer = new KieServerContainer(network);
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    private KieServicesClient kieServicesClient;

    @Test
    public void firstTest() throws Exception {
        logger.info("KIE SERVER started at port "+kieServer.getKiePort());
        logger.info("LDAP started at port "+ldap.getLdapPort());
        
    	setupClient("krisv", "krisv3");
       
    }

    private void setupClient(String user, String password) {
        ReleaseId releaseId = new ReleaseId(GROUP_ID, ARTIFACT_ID, VERSION);
        String serverUrl = "http://localhost:" + kieServer.getKiePort() + "/kie-server/services/rest/server";
        KieServicesConfiguration configuration = KieServicesFactory.newRestConfiguration(serverUrl, user, password);
        
        configuration.setTimeout(60000);
        configuration.setMarshallingFormat(MarshallingFormat.JSON);
        kieServicesClient =  KieServicesFactory.newKieServicesClient(configuration);
        
        KieContainerResource resource = new KieContainerResource(containerId, releaseId);
        resource.setContainerAlias(containerId);
        kieServicesClient.createContainer(containerId, resource);
    }
    
}

