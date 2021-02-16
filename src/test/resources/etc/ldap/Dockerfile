# Multi-stage Dockerfile

# First, build kjar from a maven image

FROM maven:3.6.3-openjdk-8-slim as builder
LABEL autodelete="true"
COPY etc/kjars/ /etc/kjars/
RUN mvn --file /etc/kjars/ldap-sample/pom.xml --batch-mode install -DskipTests

#########################################################
# Dockerfile that provides the image for KIE Server
#########################################################

FROM jboss/jbpm-server-full:latest
#FROM jboss/kie-server:latest
#FROM jboss/kie-server-showcase:latest

COPY --from=builder /root/.m2 /opt/jboss/.m2

ENV KIE_SERVER_PROFILE standalone

ENV JAVA_OPTS -Xms256m -Xmx2048m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=512m -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 \
 -Dorg.uberfire.domain=ldap -Dorg.jbpm.ht.callback=ldap -Dorg.jbpm.ht.userinfo=ldap \
 -Djbpm.user.info=file:///$JBOSS_HOME/standalone/configuration/jbpm.user.info.properties \
 -Djbpm.usergroup.callback=file:///$JBOSS_HOME/standalone/configuration/jbpm.usergroup.callback.properties \
 -Dorg.kie.controller.ping.alive.timeout=5000000 \
 -Dguard.role=President

ADD etc/jbpm.user.info.properties $JBOSS_HOME/standalone/configuration/jbpm.user.info.properties
ADD etc/jbpm.usergroup.callback.properties $JBOSS_HOME/standalone/configuration/jbpm.usergroup.callback.properties

ADD etc/jbpm-custom.cli $JBOSS_HOME/bin/jbpm-custom.cli

USER root

RUN chmod +x $JBOSS_HOME/bin/jboss-cli.sh
RUN chown jboss:jboss $JBOSS_HOME/bin/jboss-cli.sh $JBOSS_HOME/bin/jbpm-custom.cli $JBOSS_HOME/standalone/configuration/jbpm.user.info.properties $JBOSS_HOME/standalone/configuration/jbpm.usergroup.callback.properties

USER jboss

RUN $JBOSS_HOME/bin/jboss-cli.sh --file=$JBOSS_HOME/bin/jbpm-custom.cli && \
rm -rf $JBOSS_HOME/standalone/configuration/standalone_xml_history/current

WORKDIR $JBOSS_HOME/bin/

CMD ["./start_jbpm-wb.sh"]
#CMD ["./start_kie-wb.sh"]

#CMD ["./start_kie-server.sh"]
