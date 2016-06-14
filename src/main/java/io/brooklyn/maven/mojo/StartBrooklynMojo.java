/*
 * Copyright 2013-2014 by Cloudsoft Corporation Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.brooklyn.maven.mojo;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.brooklyn.rest.api.ServerApi;
import org.apache.brooklyn.rest.client.BrooklynApi;
import org.apache.brooklyn.util.repeat.Repeater;
import org.apache.brooklyn.util.text.Strings;
import org.apache.brooklyn.util.time.Duration;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.google.common.annotations.VisibleForTesting;

import io.brooklyn.maven.fork.ForkOptions;
import io.brooklyn.maven.fork.ForkedServer;
import io.brooklyn.maven.fork.ProjectDependencySupplier;

/**
 * Run a Brooklyn server.
 */
@Mojo(name = "start-server",
        defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
        configurator = "include-project-dependencies",
        requiresDependencyResolution = ResolutionScope.TEST)
public class StartBrooklynMojo extends AbstractBrooklynMojo {

    private static final String SERVER_PORT_PROPERTY = "brooklyn.port";
    private static final String PLUGIN_NAME = "brooklyn-maven-plugin";

    @Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
    private ArtifactRepository localRepository;

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Component
    private ProjectDependencySupplier dependencySupplier;

    @Component
    private BuildPluginManager pluginManager;

    /**
     * The main class of the forked process.
     */
    @Parameter(
            property = "brooklyn.main",
            defaultValue = "org.apache.brooklyn.cli.Main",
            required = true)
    private String mainClass;

    /**
     * The main command for the process.
     */
    @Parameter(
            property = "brooklyn.launchCommand",
            defaultValue = "launch",
            required = true)
    private String launchCommand;

    /**
     * The IP address of the NIC to bind the Brooklyn Management Console to.
     */
    @Parameter(
            property = "brooklyn.bindAddress",
            defaultValue = "127.0.0.1",
            required = true)
    private String bindAddress;

    /**
     * The port for the Brooklyn REST server to run on. An available port will
     * be chosen at random if this parameter is not given.
     */
    @Parameter(
            property = SERVER_PORT_PROPERTY)
    private String bindPort;

    /**
     * Additional arguments for the server, for example:
     * <pre>
     *   &lt;argument&gt;--catalogInitial&lt;/argument&gt;
     *   &lt;argument&gt;/path/to/custom-catalog.bom&lt;/argument&gt;
     * </pre>
     */
    @Parameter
    private List<String> arguments;

    /**
     * Additional options for the Java process, for example to start a server listening for
     * a remote debugger on port 5005:
     * <pre>
     *   &lt;javaOption&gt;-Xverify:none&lt;/javaOption&gt;
     *   &lt;javaOption&gt;-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005&lt;/javaOption&gt;
     * </pre>
     */
    @Parameter
    private List<String> javaOptions;

    /**
     * The Maven scope to include on the server's classpath. Defaults to test. Setting
     * a value that is not a standard scope will mean there are no additions to the
     * classpath and Brooklyn's main class will not be found. Consider setting {@link
     * #testOutputDirOnClasspath} to false if the value for this parameter
     * is changed.
     */
    @Parameter(
            defaultValue = Artifact.SCOPE_TEST)
    private String serverClasspathScope;

    /**
     * Whether or not the project's output directory (project.build.outputDirectory) should
     * be included on the server's classpath.
     */
    @Parameter(
            defaultValue = "true")
    private Boolean outputDirOnClasspath;

    /**
     * Whether or not the project's test output directory (project.build.testOutputDirectory)
     * should be included on the server's classpath. Configure {@link #serverClasspathScope}
     * appropriately too.
     */
    @Parameter(
            defaultValue = "true")
    private Boolean testOutputDirOnClasspath;

    /**
     * The property to set to the newly-started server's URL.
     */
    @Parameter(
            defaultValue = "brooklyn.server")
    private String serverUrlProperty;

    /**
     * Indicates whether the goal should wait for the started server to report itself as running
     * (ascertained by polling its REST API) before returning.
     */
    @Parameter(
            defaultValue = "true")
    private boolean waitForServerUp;

    /**
     * The user to connect to the Brooklyn server as.
     */
    @Parameter(property = "brooklyn.user")
    protected String username;

    /**
     * The password for the user at the Brooklyn server.
     */
    @Parameter(property = "brooklyn.password")
    protected String password;

    /**
     * Constructor for use by Maven/Guice.
     */
    StartBrooklynMojo() {
        this(null, null, null, null, null, null, null);
    }

    @VisibleForTesting
    StartBrooklynMojo(
            ProjectDependencySupplier dependencySupplier, String bindAddress, String bindPort,
            String mainClass, String launchCommand, String serverClasspathScope, String serverUrlProperty) {
        super();
        this.dependencySupplier = dependencySupplier;
        this.bindAddress = bindAddress;
        this.bindPort = bindPort;
        this.mainClass = mainClass;
        this.launchCommand = launchCommand;
        this.serverClasspathScope = serverClasspathScope;
        this.serverUrlProperty = serverUrlProperty;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String port = !Strings.isEmpty(bindPort) ? bindPort : reserveWebServerPort();
        getLog().info("Chosen port " + port + " for server");

        Path workDir = Paths.get(getProject().getBuild().getDirectory(), PLUGIN_NAME).toAbsolutePath();
        // Haven't found a way to inject these properties into the class.
        dependencySupplier.setLocalRepository(localRepository)
                .setOutputDirOnClasspath(Boolean.TRUE.equals(outputDirOnClasspath))
                .setTestOutputDirOnClasspath(Boolean.TRUE.equals(testOutputDirOnClasspath))
                .setScope(serverClasspathScope);
        ForkOptions options = ForkOptions.builder()
                .workDir(workDir)
                .bindAddress(bindAddress)
                .bindPort(port)
                .mainClass(mainClass)
                .launchCommand(launchCommand)
                .additionalArguments(arguments != null ? arguments : Collections.<String>emptyList())
                .javaOptions(javaOptions != null ? javaOptions : Collections.<String>emptyList())
                .classpath(dependencySupplier.get())
                .username(username)
                .password(password)
                .build();

        ForkedServer forkedServer = getForker().execute(options);
        URL serverUrl = forkedServer.getServer();
        getProject().getProperties().setProperty(serverUrlProperty, serverUrl.toString());

        if (waitForServerUp) {
            waitForServerStartOrExit(forkedServer);
            getLog().info("Server running at " + serverUrl);
            BrooklynApi api = getApi(serverUrl);
            getLog().info("Server version: " + api.getServerApi().getVersion().getVersion());
        } else {
            getLog().info("Server starting at " + serverUrl);
        }
    }

    /**
     * Uses build-helper-maven-plugin:reserve-network-port to reserve a port for the Brooklyn
     * server's web console. Sets the port in the context under {@link #SERVER_PORT_PROPERTY}.
     */
    private String reserveWebServerPort() throws MojoExecutionException {
        executeMojo(
                plugin(
                        groupId("org.codehaus.mojo"),
                        artifactId("build-helper-maven-plugin"),
                        version("1.9.1")
                ),
                goal("reserve-network-port"),
                configuration(
                        element(name("portNames"), element(name("portName"), SERVER_PORT_PROPERTY))
                ),
                executionEnvironment(
                        getProject(),
                        session,
                        pluginManager
                )
        );
        return getProject().getProperties().getProperty(SERVER_PORT_PROPERTY);
    }

    protected BrooklynApi getApi(URL server) {
        if (username != null && password != null) {
            return new BrooklynApi(server, username, password);
        } else {
            return new BrooklynApi(server);
        }
    }

    /**
     * Waits for the given endpoint to respond true to {@link ServerApi#isUp}
     * or for the forked process to have exited.
     */
    private void waitForServerStartOrExit(final ForkedServer forkedServer) throws MojoFailureException {
        final URL url = forkedServer.getServer();
        final ServerApi api = getApi(url).getServerApi();
        getLog().info("Waiting for server at " + url + " to be ready within " + getTimeout());
        boolean isUp = Repeater.create("Waiting for server at " + url + " to be ready within " + getTimeout())
                .every(Duration.ONE_SECOND)
                .limitTimeTo(getTimeout())
                .until(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        // The order of these is important - if forkedServer.hasExited() then api.isUp()
                        // will throw an exception! The exception is thrown by RESTEasy.
                        return forkedServer.hasExited() || api.isUp();
                    }
                })
                .run();
        if (forkedServer.hasExited()) {
            throw new MojoFailureException("Forked server exited unexpectedly (exit code " + forkedServer.getExitCode() + ")");
        } else if (!isUp) {
            throw new MojoFailureException("Server at " + url + " does not appear to be running after " + getTimeout());
        }
    }

}
