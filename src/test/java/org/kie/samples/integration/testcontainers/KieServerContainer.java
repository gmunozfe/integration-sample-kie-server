package org.kie.samples.integration.testcontainers;

import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class KieServerContainer extends GenericContainer<KieServerContainer>{

	private static Logger logger = LoggerFactory.getLogger(KieServerContainer.class);

	private static final int KIE_HTTP_PORT = 8080;
	//private static final int KIE_EJB_PORT = 4447;
	
	public KieServerContainer(Network network, Map<String,String> args) {
	  super( new ImageFromDockerfile()
           .withBuildArg("IMAGE_NAME", args.get("IMAGE_NAME"))
           .withFileFromClasspath("etc/jbpm-custom.cli", "etc/ldap/jbpm-custom-"+args.get("SERVER")+".cli")
           .withFileFromClasspath("etc/jbpm.user.info.properties", "etc/jbpm.user.info.properties")
           .withFileFromClasspath("etc/jbpm.usergroup.callback.properties", "etc/jbpm.usergroup.callback.properties")
           .withFileFromClasspath("etc/kjars", "etc/kjars")
           .withFileFromClasspath("Dockerfile", "etc/ldap/Dockerfile"));
	
	  withEnv("START_SCRIPT", args.get("START_SCRIPT"));
	  withNetwork(network);
	  withNetworkAliases("kie-server");
      withExposedPorts(KIE_HTTP_PORT/*, KIE_EJB_PORT*/);
      withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("KIE-LOG"));
      waitingFor(Wait.forLogMessage(".*WildFly.*started in.*", 1).withStartupTimeout(Duration.ofMinutes(5L)));
	}
	
	public Integer getKieHttpPort() {
	    return this.getMappedPort(KIE_HTTP_PORT);
    }
	
	public String getKieHost() {
	    return this.getContainerIpAddress();
    }
}
