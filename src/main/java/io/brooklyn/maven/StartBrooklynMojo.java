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
package io.brooklyn.maven;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.brooklyn.rest.api.ServerApi;
import org.apache.brooklyn.rest.client.BrooklynApi;
import org.apache.brooklyn.util.repeat.Repeater;
import org.apache.brooklyn.util.text.Strings;
import org.apache.brooklyn.util.time.Duration;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.cli.CommandLineCallable;
import org.apache.maven.shared.utils.cli.CommandLineException;
import org.apache.maven.shared.utils.cli.CommandLineUtils;
import org.apache.maven.shared.utils.cli.Commandline;
import org.apache.maven.shared.utils.cli.DefaultConsumer;
import org.apache.maven.shared.utils.cli.StreamConsumer;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Files;

import io.brooklyn.maven.util.Context;

/**
 * Run a Brooklyn server.
 */
@Mojo(name = "start-server",
        defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
        configurator = "include-project-dependencies",
        requiresDependencyResolution = ResolutionScope.TEST)
public class StartBrooklynMojo extends AbstractMojo {

    private static final String SERVER_PORT_PROPERTY = "brooklyn.port";
    private static final String PLUGIN_NAME = "brooklyn-maven-plugin";

    @Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
    private ArtifactRepository localRepository;

    @Component
    private MavenProject project;

    @Component
    private MavenSession session;

    @Component
    private BuildPluginManager pluginManager;

    /**
     * The main class of the forked process.
     */
    @Parameter(property = "brooklyn.main",
            defaultValue = "org.apache.brooklyn.cli.Main",
            required = true)
    private String mainClass;

    /**
     * The IP address of the NIC to bind the Brooklyn Management Console to.
     */
    @Parameter(property = "brooklyn.bindAddress",
            defaultValue = "127.0.0.1",
            required = true)
    private String bindAddress;

    /**
     * The port for the Brooklyn REST server to run on. An available port will
     * be chosen at random if this parameter is not given.
     */
    @Parameter(property = SERVER_PORT_PROPERTY)
    private String bindPort;

    /**
     * The property to set to the newly-started server's URL.
     */
    @Parameter(defaultValue = "brooklyn.server")
    private String serverUrlProperty;

    @Parameter(property = "waitForServerUp",
            defaultValue = "true")
    private boolean waitForServerUp;

    @Override
    public void execute() throws MojoExecutionException {
        String port = !Strings.isEmpty(bindPort) ? bindPort : reserveWebServerPort();
        getLog().info("Chosen port " + port + " for server");

        Commandline cl = buildCommandLine(port);

        // DefaultConsumer simply calls System.out.println.
        StreamConsumer sysout = new DefaultConsumer();
        StreamConsumer syserr = sysout;
        // todo would like to inject but surprising to user to have to give the argument to the start goal.
        final int shutdownTimeout = 60;
        CommandLineCallable callable;
        getLog().debug("Executing: " + cl);
        try {
            // First null: no stdin. Second: no runnable after termination.
            callable = CommandLineUtils.executeCommandLineAsCallable(cl, null, sysout, syserr, shutdownTimeout, null);
        } catch (CommandLineException e) {
            throw new MojoExecutionException("Error forking server", e);
        }

        final String serverUrl = "http://" + bindAddress + ":" + port;
        Context.setForkedCallable(project, serverUrl, callable);
        project.getProperties().setProperty(serverUrlProperty, serverUrl);

        if (waitForServerUp) {
            waitForServerStart(serverUrl);
            getLog().info("Server running at " + serverUrl);
            BrooklynApi api = new BrooklynApi(serverUrl);
            getLog().info("Server version: " + api.getServerApi().getVersion().getVersion());
        }
    }

    private Commandline buildCommandLine(String port) throws MojoExecutionException {
        Commandline cl = new Commandline();
        cl.addSystemEnvironment();
        // todo: inject other environment variables
        cl.setWorkingDirectory(createOutputDirectory());
        cl.setExecutable("java");
        cl.createArg().setValue("-classpath");
        cl.createArg().setValue(buildClasspath());
        // todo: inject
        cl.createArg().setValue("-Droot.level=OFF");
        cl.createArg().setValue(mainClass);
        cl.createArg().setValue("launch");
        cl.createArg().setValue("--bindAddress");
        cl.createArg().setValue(bindAddress);
        cl.createArg().setValue("--port");
        cl.createArg().setValue(port);
        return cl;
    }

    /**
     * Creates and returns the path to a directory in the project's build directory.
     */
    private String createOutputDirectory() throws MojoExecutionException {
        Path workingDir = Paths.get(project.getBuild().getDirectory(), PLUGIN_NAME).toAbsolutePath();
        Path confDir = workingDir.resolve("conf");
        confDir.toFile().mkdirs();
        // todo store in resources
        String logback = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<configuration>\n" +
                "    <property name=\"logging.basename\" scope=\"context\" value=\"brooklyn\" />\n" +
                "    <property name=\"logging.dir\" scope=\"context\" value=\"./\" />\n" +
                "    <include resource=\"logback-main.xml\"/>\n" +
                "</configuration>";
        try {
            Files.write(logback, confDir.resolve("logback.xml").toFile(), Charsets.UTF_8);
        } catch (IOException e) {
            throw new MojoExecutionException("Error writing logback configuration", e);
        }

        return workingDir.toString();
    }

    /**
     * @return A classpath of the test output directory, then the build output directory,
     * then all artifacts in the test scope.
     */
    private String buildClasspath() {
        String separator = System.getProperty("path.separator");
        project.setArtifactFilter(new ScopeArtifactFilter(Artifact.SCOPE_TEST));
        Set<Artifact> artifacts = project.getArtifacts();
        final String repoBaseDir = localRepository.getBasedir();

        Set<String> urls = new HashSet<>();
        for (Artifact artifact : artifacts) {
            Path path = Paths.get(repoBaseDir, localRepository.pathOf(artifact));
            urls.add(path.toAbsolutePath().toString());
        }

        // contains logback
        Path conf = Paths.get(project.getBuild().getDirectory(), PLUGIN_NAME, "conf").toAbsolutePath();
        String testOut = project.getBuild().getTestOutputDirectory();
        String buildOut = project.getBuild().getOutputDirectory();
        StringBuilder cp = new StringBuilder(conf.toString()).append(separator)
                .append(testOut).append(separator)
                .append(buildOut).append(separator);
        Joiner.on(separator).appendTo(cp, urls);
        return cp.toString();
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
                    project,
                    session,
                pluginManager
            )
        );
        return project.getProperties().getProperty(SERVER_PORT_PROPERTY);
    }

    /**
     * Waits for the given endpoint to respond true to {@link ServerApi#isUp}.
     */
    private void waitForServerStart(String url) {
        final ServerApi api = new BrooklynApi(url).getServerApi();
        // todo: inject timeout
        getLog().info("Waiting for server at " + url + " to be ready");
        Duration timeout = Duration.ONE_MINUTE;
        boolean isUp = Repeater.create("Waiting for server at " + url + " to be ready")
                .every(Duration.ONE_SECOND)
                .limitTimeTo(timeout)
                .until(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return api.isUp();
                    }
                })
                .run();
        if (!isUp) {
            getLog().warn("Server at " + url + " does not appear to be running after " + timeout);
        }
    }

}
