package org.kie.samples.integration;


import org.junit.ClassRule;
import org.junit.Test;
import org.kie.samples.integration.testcontainers.KieServerContainer;
import org.kie.samples.integration.testcontainers.LdapContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker=true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class KieIntegrationTest {
    private static Logger logger = LoggerFactory.getLogger(KieIntegrationTest.class);

    public static Network network = Network.newNetwork();

    
    @ClassRule
    public static LdapContainer ldap = new LdapContainer(network);

    @ClassRule
    public static KieServerContainer kieServer = new KieServerContainer(network);
    
    @Value("${server.port:8080}")
    private String serverPort;

    @Test
    public void firstTest() throws Exception {
        logger.info("Sleeping..."+kieServer.getKiePort());
    	logger.info("LDAP..."+ldap.getLdapPort());
        Thread.sleep(10000000);

        
    }

    
}

