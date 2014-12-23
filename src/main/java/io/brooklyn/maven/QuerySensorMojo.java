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
import java.util.Map;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;

/**
 * Fetch the value of a sensor on entities at a given server whose types
 * match a regular expression.
 */
@Mojo(name = "sensor",
        defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class QuerySensorMojo extends AbstractInvokeBrooklynMojo {

    /**
     * The ID of the application whose entities should be queried.
     */
    @Parameter(
            required = true,
            property = "brooklyn.app")
    private String application;

    /**
     * The name of the sensor to query.
     */
    @Parameter(
            required = true,
            property = "brooklyn.sensor")
    private String sensor;

    /**
     * The property to set to the sensor's value.
     */
    @Parameter(defaultValue = "brooklyn.sensor")
    private String propertyName;

    /**
     * Regular expression to match entities by type.
     */
    @Parameter(
            property = "brooklyn.entityTypeRegex",
            defaultValue = ".*")
    private String typeRegex;

    /**
     * Constructor for use by Maven/Guice.
     */
    QuerySensorMojo() {
        this(null, null, null, null, null);
    }

    public QuerySensorMojo(URL server, String application, String sensor, String propertyName, String typeRegex) {
        super(server);
        this.application = application;
        this.sensor = sensor;
        this.propertyName = propertyName;
        this.typeRegex = typeRegex;
    }

    @Override
    public void execute() throws MojoFailureException {
        Map<String, Object> matches = getApi().getEntityApi().getDescendantsSensor(
                application, application, sensor, typeRegex);
        getLog().info("Matches: " + Joiner.on(", ").withKeyValueSeparator("=").join(matches));
        String value;
        if (matches.keySet().size() == 1) {
            value = Iterables.getOnlyElement(matches.values()).toString();
        } else {
            value = Iterables.toString(matches.values());
        }
        getLog().debug("Setting " + propertyName + " to " + value);
        getProject().getProperties().setProperty(propertyName, value);
    }

}
