Injecting sensors into your build
=================================

The [example-pom](https://github.com/brooklyncentral/brooklyn-maven-plugin/tree/master/src/test/projects/example-app)
project:

* Deploys this blueprint:

```yaml
name: My Blueprint
location: localhost
services:
- type: brooklyn.entity.basic.EmptySoftwareProcess
- type: brooklyn.entity.webapp.tomcat.TomcatServer
```

* Queries Brooklyn for the main URL of the Tomcat application (the
  `webapp.url` sensor).
* Uses `maven-antrun-plugin` to echo these values to the console.
* Stops the deployment before Maven exits.

The important bits of the pom are:

```xml
<plugin>
    <groupId>io.brooklyn.maven</groupId>
    <artifactId>brooklyn-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>Deploy blueprint</id>
            <goals>
                <goal>deploy</goal>
            </goals>
            <configuration>
                <server>${server}</server>
                <blueprint>${project.basedir}/blueprint.yaml</blueprint>
                <propertyName>appId</propertyName>
            </configuration>
        </execution>
        <execution>
            <id>Query sensor value</id>
            <phase>pre-integration-test</phase>
            <goals>
                <goal>sensor</goal>
            </goals>
            <configuration>
                <server>${server}</server>
                <application>${appId}</application>
                <sensor>webapp.url</sensor>
                <typeRegex>.*Tomcat.*</typeRegex>
                <propertyName>sensorVal</propertyName>
            </configuration>
        </execution>
        <execution>
            <id>Stop the application</id>
            <goals>
                <goal>stop</goal>
            </goals>
            <configuration>
                <server>${server}</server>
                <application>${appId}</application>
            </configuration>
        </execution>
    </executions>
</plugin>
```

You can test this project by starting Brooklyn and running `mvn clean install`
from the project's `example-app` directory. If Brooklyn starts on any port
other than 8081 then run with `-Dserver=http://host:port`.

At the end of the build you should see output like:

```
[INFO] --- maven-antrun-plugin:1.3:run (default) @ test ---
[INFO] Executing tasks
    [echo] Maven plugin example results:
    [echo] Application: T0tERELL
    [echo] Sensor value: http://127.0.0.1:8080/
```

