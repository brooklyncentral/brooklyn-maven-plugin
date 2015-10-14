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

import java.net.URL;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.utils.cli.CommandLineCallable;

import com.google.common.base.Optional;

import io.brooklyn.maven.util.Context;

/**
 * Instruct a Brooklyn server to shut down.
 * <p>
 * If the server was started by the {@link StartBrooklynMojo start goal} the plugin
 * will wait for the forked process to exit.
 */
@Mojo(name = "stop-server",
        defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST)
public class StopBrooklynMojo extends AbstractInvokeBrooklynMojo {

    /**
     * Instruct the Brooklyn server to terminate all running applications before
     * terminating itself.
     */
    @Parameter(
            property = "brooklyn.stopApplications",
            defaultValue = "true")
    private Boolean stopApplications;

    /**
     * Instruct the Brooklyn server to force its shutdown even when errors were
     * thrown while terminating applications.
     */
    @Parameter(
            property = "brooklyn.forceShutdown",
            defaultValue = "true")
    private Boolean forceShutdownOnError;

    /**
     * The maximum time to wait for the server to shutdown. Defaults to forever.
     */
    @Parameter(
            property = "brooklyn.timeout",
            defaultValue = "0")
    private String shutdownTimeout;

    /**
     * Constructor for use by Maven/Guice.
     */
    StopBrooklynMojo() {
        this(null);
    }

    public StopBrooklynMojo(URL server) {
        super(server);
        this.stopApplications = true;
        this.forceShutdownOnError = true;
        this.shutdownTimeout = "0";
    }

    @Override
    public void execute() {
        getLog().info("Stopping server at " + server +
                ", timeout=" + shutdownTimeout +
                ", stopApps=" + stopApplications +
                ", force=" + forceShutdownOnError);

        getApi().getServerApi().shutdown(stopApplications, forceShutdownOnError, shutdownTimeout, shutdownTimeout, shutdownTimeout, null);
        Optional<CommandLineCallable> callable = Context.getForkedCallable(getProject(), server.toString());

        if (callable.isPresent()) {
            try {
                // Waits for the forked process to exit.
                getLog().debug("Waiting for forked process to complete");
                callable.get().call();
                getLog().debug("Forked process complete");
            } catch (Exception e) {
                getLog().warn("Exception waiting for forked process to complete", e);
            }
        } else {
            // Possible if the start-server goal was not used.
            getLog().debug("Cannot wait for server to exit: no callable context.");
        }
    }
}
