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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.apache.brooklyn.util.collections.Jsonya;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.Test;

import com.squareup.okhttp.mockwebserver.RecordedRequest;

public class QuerySensorMojoTest extends AbstractBrooklynMojoTest {

    private static final String APPLICATION = "abcdef";
    private static final String SENSOR = "app.sensor";
    private static final String TYPE_REGEX = "foo.*";
    private static final String PROJECT_PROPERTY = "build.property";

    @Test
    public void testMojoQueriesSensorValueAndSetsPropertyOnProject() throws Exception {
        final String sensorVal = "Bananaman";
        server.enqueue(newJsonResponse()
                .setBody(Jsonya.newInstance().put(APPLICATION, sensorVal).toString()));
        server.play();

        MavenProject mavenProject = new BrooklynMavenProjectStub();
        QuerySensorMojo mojo = new QuerySensorMojo(server.getUrl("/"), APPLICATION, SENSOR, PROJECT_PROPERTY, TYPE_REGEX);
        mojo.setProject(mavenProject);
        executeMojoWithTimeout(mojo);

        RecordedRequest request = server.takeRequest(1, TimeUnit.MILLISECONDS);
        String expectedPath = String.format("/v1/applications/%s/entities/%s/descendants/sensor/%s?typeRegex=%s",
                APPLICATION, APPLICATION, SENSOR, TYPE_REGEX);
        assertEquals(expectedPath, request.getPath());
        assertEquals("GET", request.getMethod());
        assertEquals(sensorVal, mavenProject.getProperties().getProperty(PROJECT_PROPERTY));
        assertEquals(1, server.getRequestCount());
    }

    @Test
    public void testBuildFailsIfNoEntitiesMatchRegex() throws Exception {
        server.enqueue(newJsonResponse().setBody("{}"));
        server.play();
        QuerySensorMojo mojo = new QuerySensorMojo(server.getUrl("/"), APPLICATION, SENSOR, PROJECT_PROPERTY, TYPE_REGEX);
        mojo.setFailIfNoMatches();
        try {
            executeMojoWithTimeout(mojo);
            fail("Expected exception running query goal with no results");
        } catch (MojoFailureException e) {
            assertTrue("Expected exception message to start 'No entities' and end 'have a value for <sensor>, was: " + e.getMessage(),
                    e.getMessage().startsWith("No entities") && e.getMessage().endsWith("have a value for " + SENSOR));
        }
    }

    @Test
    public void testWaitsForApplicationToBeRunning() throws Exception {
        server.enqueue(applicationStatusResponse("STARTING"));
        server.enqueue(applicationStatusResponse("STARTING"));
        server.enqueue(applicationStatusResponse("RUNNING"));
        server.enqueue(newJsonResponse().setBody(Jsonya.newInstance().put(APPLICATION, "bob").toString()));
        server.play();

        MavenProject mavenProject = new BrooklynMavenProjectStub();
        QuerySensorMojo mojo = new QuerySensorMojo(server.getUrl("/"), APPLICATION, SENSOR, PROJECT_PROPERTY, TYPE_REGEX);
        mojo.setPollPeriod(1, TimeUnit.MILLISECONDS);
        mojo.setProject(mavenProject);
        mojo.setWaitForRunning();
        executeMojoWithTimeout(mojo);

        assertEquals(4, server.getRequestCount());
    }

}
