package org.kie.samples.integration.testcontainers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;

public class LdapContainer extends GenericContainer<LdapContainer>{

	private static Logger logger = LoggerFactory.getLogger(LdapContainer.class);

    private static final int LDAP_PORT = 389;
    
    
    /**
     * Create a LdapContainer by passing the full docker image name
     *
     */
    public LdapContainer(Network network) {
       super("osixia/openldap:1.4.0");
       withNetwork(network);
       withNetworkAliases("ldap-alias");
       withExposedPorts(LDAP_PORT);
       withEnv("LDAP_DOMAIN","jbpm.org");
       withEnv("LDAP_ADMIN_PASSWORD","admin");
       withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("LDAP-LOG"));
       withClasspathResourceMapping("etc/ldap/jbpm.ldif", "/container/service/slapd/assets/config/bootstrap/ldif/custom/jbpm.ldif", BindMode.READ_ONLY);
       withCommand("--copy-service");
    }

    public Integer getLdapPort() {
	    return this.getMappedPort(LDAP_PORT);
    }
    
}
