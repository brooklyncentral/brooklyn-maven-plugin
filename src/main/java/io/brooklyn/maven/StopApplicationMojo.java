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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.google.common.collect.ImmutableMap;

/**
 * Instruct a Brooklyn server to stop the application with the given ID.
 */
@Mojo(name = "stop",
        defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST)
public class StopApplicationMojo extends AbstractInvokeBrooklynMojo {

    /**
     * The ID of the application to be stopped.
     */
    @Parameter(
            required = true,
            property = "brooklyn.app")
    private String application;

    /**
     * Constructor for use by Maven/Guice.
     */
    StopApplicationMojo() {
        this(null, null);
    }

    public StopApplicationMojo(URL server, String application) {
        super(server);
        this.application = application;
    }

    @Override
    public void execute() {
        getLog().info("Stopping application " + application);
        try {
            String timeout = String.valueOf(getTimeout().toMilliseconds());
            getApi().getEffectorApi().invoke(application, application, "stop", timeout,
                    ImmutableMap.<String, Object>of());
        } catch (Exception e) {
            getLog().warn("Exception stopping application", e);
        }
    }

}
