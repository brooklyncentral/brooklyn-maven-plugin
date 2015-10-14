Injecting sensors into your build
=================================

The [example-pom](https://github.com/brooklyncentral/brooklyn-maven-plugin/tree/master/src/test/projects/example-app)
project:

* Runs a Brooklyn 0.8.0-incubating server
* Deploys this blueprint:

```yaml
name: My Blueprint
location: localhost
services:
- type: org.apache.brooklyn.entity.software.base.EmptySoftwareProcess
- type: org.apache.brooklyn.entity.webapp.tomcat.TomcatServer
```

* Queries Brooklyn for the main URL of the Tomcat application (the
  `webapp.url` sensor).
* Uses `maven-antrun-plugin` to echo these values to the console.
* Stops the deployed application.
* Stops the Brooklyn server before Maven exits.

The important bits of the pom are:

```xml
<dependencies>
    <dependency>
        <groupId>org.apache.brooklyn</groupId>
        <artifactId>brooklyn-all</artifactId>
        <version>0.8.0-incubating</version>
    </dependency>
</dependencies>

<plugin>
    <groupId>io.brooklyn.maven</groupId>
    <artifactId>brooklyn-maven-plugin</artifactId>
    <version>0.3.0-SNAPSHOT</version>
    <executions>
        <execution>
            <id>Run Brooklyn</id>
            <goals>
                <goal>start-sever</goal>
                <goal>deploy</goal>
                <goal>sensor</goal>
                <goal>stop</goal>
                <goal>stop-server</goal>
            </goals>
            <configuration>
                <blueprint>${project.basedir}/blueprint.yaml</blueprint>
                <sensor>webapp.url</sensor>
                <typeRegex>.*Tomcat.*</typeRegex>
            </configuration>
        </execution>
    </executions>
</plugin>
<plugin>
    <artifactId>maven-antrun-plugin</artifactId>
    <executions>
        <execution>
            <phase>integration-test</phase>
            <goals>
                <goal>run</goal>
            </goals>
            <configuration>
                <tasks>
                    <echo>Maven plugin example results:</echo>
                    <echo>Server was running at ${brooklyn.server}</echo>
                    <echo>Application: ${brooklyn.app}</echo>
                    <echo>Sensor value: ${brooklyn.sensor}</echo>
                </tasks>
            </configuration>
        </execution>
    </executions>
</plugin>
```

You can test this project by starting Brooklyn and running `mvn clean install`
from the project's `example-app` directory.

At the end of the build you should see output like:

```
[INFO] --- maven-antrun-plugin:1.3:run (default) @ test ---
[INFO] Executing tasks
    [echo] Maven plugin example results:
    [echo] Server was running at http://127.0.0.1:57641/
    [echo] Application: T0tERELL
    [echo] Sensor value: http://127.0.0.1:8080/
```

