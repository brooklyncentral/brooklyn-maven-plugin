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
import java.util.List;
import java.util.concurrent.Callable;
import javax.ws.rs.core.Response;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.google.common.base.Joiner;
import com.google.common.io.Files;

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
        this.propertyName = propertyName;
    }

    public void execute() throws MojoFailureException {
        getLog().debug("Working with server at " + server);

        // Propagates all non-mojo exceptions as MojoFailureExceptions
        try {
            String loadedBlueprint = loadBlueprint();
            getLog().debug("Blueprint:\n" + loadedBlueprint);
            Response r = getApi().getApplicationApi().createFromYaml(loadedBlueprint);
            // TODO: Worth acting on non 2xx response code?
            getLog().debug("Deploy blueprint: responseCode=" + r.getStatus());
            final TaskSummary task = BrooklynApi.getEntity(r, TaskSummary.class);
            final String appId = task.getEntityId();
            getLog().info("Waiting for application " + appId + " to be running");
            boolean isAppRunning = Repeater.create("Waiting for application " + appId + " to be running")
                    .every(getPollPeriod())
                    .limitTimeTo(getTimeout())
                    .until(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            ApplicationSummary app = getApi().getApplicationApi().get(appId);
                            return Status.RUNNING.equals(app.getStatus());
                        }
                    })
                    .run();

            if (isAppRunning) {
                getLog().info("Application " + appId + " is running");
                if (propertyName != null) {
                    getProject().getProperties().setProperty(propertyName, task.getEntityId());
                }
            } else {
                throw new MojoFailureException("Application " + appId + " is not running within " + getTimeout());
            }
        } catch (MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoFailureException("Exception deploying blueprint", e);
        }
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
            throw new IllegalArgumentException("URL not implemented yet");
        }
    }

    private String readFile(File file) throws MojoFailureException {
        try {
            // todo: inject encoding from the pom
            List<String> lines = Files.readLines(file, Charset.forName("UTF-8"));
            return Joiner.on('\n').join(lines);
        } catch (Exception e) {
            throw new MojoFailureException("Failed to load " + file.getAbsolutePath(), e);
        }
    }

}
