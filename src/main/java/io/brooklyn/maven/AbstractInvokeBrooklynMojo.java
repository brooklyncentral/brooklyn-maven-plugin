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

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.Response;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.google.common.annotations.VisibleForTesting;

import brooklyn.management.ha.ManagementNodeState;
import brooklyn.rest.client.BrooklynApi;
import brooklyn.util.time.Duration;

/**
 * An abstract class for Mojos that invoke actions on an existing Brooklyn server.
 */
public abstract class AbstractInvokeBrooklynMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /**
     * The URL of the Brooklyn server to communicate with.
     */
    @Parameter(
            required = true,
            property = "brooklyn.url")
    protected URL server;

    /**
     * The duration mojos should wait for actions at Brooklyn to complete.
     */
    @Parameter(
            property = "brooklyn.timeout",
            defaultValue = "5")
    private Integer timeout;

    /**
     * The unit associated with {@link #timeout}.
     */
    @Parameter(
            property = "brooklyn.timeoutUnit",
            defaultValue = "MINUTES")
    private TimeUnit timeoutUnit;

    /**
     * The period that should be waited between successive polls of the Brooklyn server.
     */
    @Parameter(
            property = "brooklyn.pollPeriod",
            defaultValue = "5")
    private Integer pollPeriod;

    /**
     * The unit associated with {@link #pollPeriod}.
     */
    @Parameter(
            property = "brooklyn.pollUnit",
            defaultValue = "SECONDS")
    private TimeUnit pollUnit;

    private BrooklynApi api;

    /**
     * Constructor for use by Maven/Guice.
     */
    AbstractInvokeBrooklynMojo() {
        this(null);
    }

    public AbstractInvokeBrooklynMojo(URL server) {
        this.server = server;
        this.timeout = 5;
        this.timeoutUnit = TimeUnit.SECONDS;
        this.pollPeriod = 1;
        this.pollUnit = TimeUnit.SECONDS;
    }

    protected BrooklynApi getApi() {
        if (api == null) {
            api = new BrooklynApi(server);
        }
        return api;
    }

    /**
     * @return The duration that mojos should wait between successive polls of
     *      the server when waiting for state to change.
     */
    protected Duration getPollPeriod() {
        return Duration.of(pollPeriod, pollUnit);
    }

    protected MavenProject getProject() {
        return project;
    }

    protected Duration getTimeout() {
        return Duration.of(timeout, timeoutUnit);
    }

    void setPollPeriod(int period, TimeUnit unit) {
        this.pollPeriod = checkNotNull(period, "period");
        this.pollUnit = checkNotNull(unit, "unit");
    }

    @VisibleForTesting
    void setProject(MavenProject project) {
        this.project = project;
    }

    void setTimeout(int timeout, TimeUnit unit) {
        this.timeout = checkNotNull(timeout, "timeout");
        this.timeoutUnit = checkNotNull(unit, "unit");
    }

    /**
     * @throws org.apache.maven.plugin.MojoFailureException if the server's HA state is not master.
     */
    protected void checkServerMaster() throws MojoFailureException {
        ManagementNodeState nodeState = getApi().getServerApi().getHighAvailabilityNodeState();
        if (!ManagementNodeState.MASTER.equals(nodeState)) {
            throw new MojoFailureException("Expected server to be HA master, is " + nodeState.name());
        }
    }

    protected boolean isUnhealthyResponse(Response response) {
        return response.getStatus() < 200 || response.getStatus() >= 300;
    }
}
