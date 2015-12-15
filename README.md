Brooklyn Maven Plugin
=====================

A Maven plugin to help test [Apache
Brooklyn](https://brooklyn.apache.org/) blueprints. For full
documentation see [the plugin website]
(http://brooklyncentral.github.io/brooklyn-maven-plugin/index.html).


### Download

Include this plugin in your pom:
```xml
<plugin>
    <groupId>io.brooklyn.maven</groupId>
    <artifactId>brooklyn-maven-plugin</artifactId>
    <version>${latestPluginVersion}</version>
</plugin>
```


### Goals

<dl>
<dt><strong>start-server</strong></dt>
<dd>
Run a Brooklyn server.
</dd>

<dl>
<dt><strong>deploy</strong></dt>
<dd>
Instruct an existing Brooklyn server to deploy the given blueprint.
</dd>

<dt><strong>sensor</strong></dt>
<dd>
Fetch the value of a sensor on entities at a given server whose types match
a regular expression.
</dd>

<dt><strong>stop</strong></dt>
<dd>
Instruct a Brooklyn server to stop the application with the given ID.
</dd>

<dt><strong>stop-server</strong></dt>
<dd>
Instruct a Brooklyn server to shut down
</dd>

<dt><strong>help</strong></dt>
<dd>
Display help information on brooklyn-maven-plugin.
</dd>
</dl>


### Example

The [example-pom](src/test/projects/example-app/pom.xml) project:

* Deploys a blueprint from a file that runs a
  [TomcatServer](https://brooklyn.apache.org/learnmore/catalog/catalog-item.html#!entities/org.apache.brooklyn.entity.webapp.tomcat.TomcatServer)
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
```

You can test this project by starting Brooklyn and running `mvn clean install`
from the `example-app` directory. If Brooklyn starts on any port other than
8081 then run with `-Dserver=http://host:port`.

At the end of the build you should see output like:
```
[INFO] --- maven-antrun-plugin:1.3:run (default) @ test ---
[INFO] Executing tasks
    [echo] Maven plugin example results:
    [echo] Server was running at http://127.0.01:8081
    [echo] Application: T0tERELL
    [echo] Sensor value: http://127.0.0.1:8080/
```

The plugin writes a `logback.xml` file to `target/brooklyn-maven-plugin/conf/` and puts it on the server's classpath.
Server logs (`brooklyn.debug.log` and `brooklyn.info.log`) are written to `target/brooklyn-maven-plugin/.

