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

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.Response;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.google.common.base.Joiner;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import brooklyn.rest.client.BrooklynApi;
import brooklyn.rest.domain.ApplicationSummary;
import brooklyn.rest.domain.Status;
import brooklyn.rest.domain.TaskSummary;
import brooklyn.util.repeat.Repeater;

/**
 * Instruct an existing Brooklyn server to deploy the given blueprint.
 */
@Mojo(name = "deploy",
        defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class DeployBlueprintMojo extends AbstractInvokeBrooklynMojo {

    /**
     * The location of the blueprint to deploy. Either a file on disk or a remote URL.
     */
    @Parameter(
            required = true,
            property = "brooklyn.blueprint")
    private String blueprint;

    /**
     * The encoding of the blueprint to deploy.
     */
    @Parameter(
            property = "brooklyn.blueprintEncoding",
            defaultValue = "UTF-8")
    private String blueprintEncoding;

    /**
     * The property to set to the application id.
     */
    @Parameter(defaultValue = "brooklyn.app")
    private String propertyName;

    /**
     * Constructor for use by Maven/Guice.
     */
    DeployBlueprintMojo() {
        this(null, null);
    }

    public DeployBlueprintMojo(URL server, String blueprint) {
        this(server, blueprint, null);
    }

    public DeployBlueprintMojo(URL server, String blueprint, String propertyName) {
        super(server);
        this.blueprint = blueprint;
        this.blueprintEncoding = "UTF-8";
        this.propertyName = propertyName;
    }

    public void execute() throws MojoFailureException {
        getLog().debug("Working with server at " + server);

        // Propagates all non-mojo exceptions as MojoFailureExceptions
        try {
            String loadedBlueprint = loadBlueprint();
            getLog().debug("Blueprint:\n" + loadedBlueprint);
            Response r = getApi().getApplicationApi().createFromYaml(loadedBlueprint);

            if (isUnhealthyResponse(r)) {
                throw new MojoFailureException("Unexpected response deploying blueprint to server: " + r.getStatus());
            } else {
                getLog().debug("Server response to deploy blueprint: " + r.getStatus());
            }

            final TaskSummary task = BrooklynApi.getEntity(r, TaskSummary.class);
            final String appId = task.getEntityId();

            getLog().info("Waiting " + getTimeout() + " from " + new Date() + " for application " + appId + " to be running");
            final AtomicReference<Status> appStatus = new AtomicReference<Status>(Status.UNKNOWN);

            // Poll Brooklyn until the deployed app is running or on fire.
            boolean finalAppStatusKnown = Repeater.create("Waiting for application " + appId + " to be running")
                    .every(getPollPeriod())
                    .limitTimeTo(getTimeout())
                    .rethrowExceptionImmediately()
                    .until(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            ApplicationSummary app = getApi().getApplicationApi().get(appId);
                            Status status = app.getStatus();
                            appStatus.set(status);
                            return Status.RUNNING.equals(status) || Status.ERROR.equals(status);
                        }
                    })
                    .run();

            switch (appStatus.get()) {
            case RUNNING:
                getLog().info("Application " + appId + " is running");
                if (propertyName != null) {
                    getProject().getProperties().setProperty(propertyName, task.getEntityId());
                    getLog().debug("Set property '" + propertyName + "' to: " + appId);
                } else {
                    getLog().info("No property to set to new application ID");
                }
                break;
            default:
                throw new MojoFailureException("Application is not running within " + getTimeout() + ". Status is: " + appStatus.get());
            }
        } catch (MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoFailureException("Exception deploying blueprint", e);
        }
    }

    void setBlueprintEncoding(String encoding) {
        this.blueprintEncoding = encoding;
    }

    private String loadBlueprint() throws MojoFailureException {
        File f = new File(blueprint);
        if (f.isFile()) {
            if (f.canRead()) {
                getLog().info("Loading blueprint from " + f.getAbsolutePath());
                return readFile(f);
            } else {
                throw new MojoFailureException("Unable to read blueprint from " + f.getAbsolutePath());
            }
        } else {
            return readUrl(blueprint);
        }
    }

    private String readFile(File file) throws MojoFailureException {
        try {
            List<String> lines = Files.readLines(file, Charset.forName(blueprintEncoding));
            return Joiner.on('\n').join(lines);
        } catch (Exception e) {
            throw new MojoFailureException("Failed to load " + file.getAbsolutePath(), e);
        }
    }

    private String readUrl(String blueprint) throws MojoFailureException {
        try {
            CharSource source = Resources.asCharSource(new URL(blueprint), Charset.forName(blueprintEncoding));
            return Joiner.on("\n").join(source.readLines());
        } catch (Exception e) {
            throw new MojoFailureException("Failed to load " + blueprint, e);
        }
    }

}
