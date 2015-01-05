Brooklyn Maven Plugin
=====================

A Maven plugin to help test your [Apache
Brooklyn](https://brooklyn.incubator.apache.org/) blueprints.

### Download

The plugin is not currently published to Maven repositories.  For now you must
clone and install this project yourself.

<!-- Enable when plugin published.
Include this dependency in your pom:
```xml
<dependency>
    <groupId>io.brooklyn.maven</groupId>
    <artifactId>brooklyn-maven-plugin</artifactId>
    <version>(insert latest version)</version>
</dependency>
```
-->

### Goals

<dl>
<dt><strong>deploy</strong></dt>
<dd>
Instruct an existing Brooklyn server to deploy the given blueprint.
</dd>

<dt><strong>sensor</strong></dt>
<dd>
Instruct a Brooklyn server to stop the application with the given ID.
</dd>

<dt><strong>stop</strong></dt>
<dd>
Fetch the value of a sensor on entities at a given server whose types match
a regular expression.
</dd>

<dt><strong>help</strong></dt>
<dd>
Display help information on brooklyn-maven-plugin.
</dd>
</dl>


### Example

The [example-pom](src/test/projects/example-app/pom.xml) project:

* Deploys a blueprint from a file that runs a
  [TomcatServer](https://brooklyn.incubator.apache.org/learnmore/catalog/entities/brooklyn.entity.webapp.tomcat.TomcatServer.html)
  and an EmptySoftwareProcess.
* Queries Brooklyn for the main URL of the Tomcat application (the
  `webapp.url` sensor).
* Uses `maven-antrun-plugin` to echo these values to the console.
* Stops the deployment before Maven exits.

The important bits of the pom are:
```xml
<plugin>
    <groupId>io.brooklyn.maven</groupId>
    <artifactId>brooklyn-maven-plugin</artifactId>
    <version>0.1.0-SNAPSHOT</version>
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
from the `example-app` directory. If Brooklyn starts on any port other than
8081 then run with `-Dserver=http://host:port`.

At the end of the build you should see output like:
```
[INFO] --- maven-antrun-plugin:1.3:run (default) @ test ---
[INFO] Executing tasks
    [echo] Maven plugin example results:
    [echo] Application: T0tERELL
    [echo] Sensor value: http://127.0.0.1:8080/
```

### Caveats

You may currently only query for entities by their type. If two or more
entities match the query goal's regular expression the property will be set to
`result1,result2,...`.

