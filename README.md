Migrating jBPM images secured by LDAP to Elytron
================================================

**If you want to know more details about this project, [read this article at KIE blog](https://blog.kie.org/2021/02/migrating-jbpm-images-secured-by-ldap-to-elytron.html).**

## Building

For building this project locally, you firstly need to have the following tools installed locally:
- git client
- Java 1.8
- Maven
- docker (because of testcontainers makes use of it).

Once you cloned the repository locally all you need to do is execute the following Maven build:

```
mvn clean install
```

for the `kie-server-showcase` scenarios (-Pkie-server, activated by default).

For `jbpm-server-full` image migration, use full profile:

```
mvn clean install -Pfull
```