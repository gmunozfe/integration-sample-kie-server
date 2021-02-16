package org.kie.samples.integration.testcontainers;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class KieServerContainer extends GenericContainer<KieServerContainer>{

	private static Logger logger = LoggerFactory.getLogger(KieServerContainer.class);

	private static final int KIE_PORT = 8080;
	
	public KieServerContainer(Network network) {
	  super( new ImageFromDockerfile()
           .withFileFromClasspath("etc/jbpm-custom.cli", "etc/ldap/jbpm-custom.cli")
           .withFileFromClasspath("etc/jbpm.user.info.properties", "etc/jbpm.user.info.properties")
           .withFileFromClasspath("etc/jbpm.usergroup.callback.properties", "etc/jbpm.usergroup.callback.properties")
           .withFileFromClasspath("etc/kjars", "etc/kjars")
           .withFileFromClasspath("Dockerfile", "etc/ldap/Dockerfile"));
	
	  withNetwork(network);
	  withNetworkAliases("kie-server");
      withExposedPorts(KIE_PORT);
      withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("KIE-LOG"));
      waitingFor(Wait.forLogMessage(".*WildFly.*started in.*", 1).withStartupTimeout(Duration.ofMinutes(10L)));
	}
	
	public Integer getKiePort() {
	    return this.getMappedPort(KIE_PORT);
    }
}
