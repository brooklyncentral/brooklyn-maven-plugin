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

/**
 * Instruct a Brooklyn server to shut down.
 */
@Mojo(name = "stop-brooklyn",
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
     * Constructor for use by Maven/Guice.
     */
    StopBrooklynMojo() {
        this(null);
    }

    public StopBrooklynMojo(URL server) {
        super(server);
        this.stopApplications = true;
        this.forceShutdownOnError = true;
    }

    @Override
    public void execute() {
        getLog().info("Stopping server at " + server);
        getApi().getServerApi().shutdown(stopApplications, forceShutdownOnError, null, null, null, null);
    }

}
