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

import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Files;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import brooklyn.util.collections.Jsonya;
import brooklyn.util.net.Networking;

public class DeployBlueprintMojoTest extends AbstractBrooklynMojoTest {

    private static final String APP_ID = "fedcba";
    private static final String NEW_APP_PROPERTY = "brooklyn.app.id";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static final String YAML = Joiner.on("\n").join(
            "name: test-blueprint",
            "location: localhost",
            "services:",
            "- type: brooklyn.entity.basic.EmptySoftwareProcess");

    private String blueprintPath;

    @Before
    public void setBlueprint() throws Exception {
        File f = folder.newFile();
        Files.write(YAML, f, Charsets.UTF_8);
        blueprintPath = f.getAbsolutePath();
    }

    @Test
    public void testPostsConfiguredBlueprintToServerAndSetsConfiguredProperty() throws Exception {
        // Just enough of a task summary to work.
        server.enqueue(newJsonResponse().setBody(Jsonya.newInstance().put("entityId", APP_ID).toString()));
        server.enqueue(applicationStatusResponse("STARTING"));
        server.enqueue(applicationStatusResponse("RUNNING"));
        server.play();

        final MavenProjectStub project = new BrooklynMavenProjectStub();
        DeployBlueprintMojo mojo = new DeployBlueprintMojo(server.getUrl("/"), blueprintPath, NEW_APP_PROPERTY);
        mojo.setPollPeriod(1, TimeUnit.MILLISECONDS);
        mojo.setProject(project);
        executeMojoWithTimeout(mojo);

        // Mojo posts blueprint
        RecordedRequest request = server.takeRequest(1, TimeUnit.MILLISECONDS);
        assertEquals("/v1/applications", request.getPath());
        assertEquals("POST", request.getMethod());
        assertEquals(YAML, new String(request.getBody()));

        // Mojo waits for blueprint to be running
        request = server.takeRequest(1, TimeUnit.MILLISECONDS);
        assertEquals("/v1/applications/" + APP_ID, request.getPath());
        assertEquals("GET", request.getMethod());
        request = server.takeRequest(1, TimeUnit.MILLISECONDS);
        assertEquals("/v1/applications/" + APP_ID, request.getPath());
        assertEquals("GET", request.getMethod());

        // No more requests and the property was set
        assertEquals(3, server.getRequestCount());

        // And the property was set on the project
        assertEquals("Property " + NEW_APP_PROPERTY + " was not set on the Maven project",
                APP_ID, project.getProperties().getProperty(NEW_APP_PROPERTY));
    }

    @Test
    public void testLoadsBlueprintFromUrl() throws Exception {
        // Pretending to be both the server hosting the blueprint and Brooklyn.
        server.enqueue(new MockResponse().setBody(YAML));
        server.enqueue(newJsonResponse().setBody(Jsonya.newInstance().put("entityId", APP_ID).toString()));
        server.enqueue(applicationStatusResponse("RUNNING"));
        server.play();

        final String blueprintPath = "/a/path/to/a/blueprint.yaml";
        final String blueprintUrl = server.getUrl(blueprintPath).toString();
        DeployBlueprintMojo mojo = new DeployBlueprintMojo(server.getUrl("/"), blueprintUrl);
        mojo.setPollPeriod(1, TimeUnit.MILLISECONDS);
        executeMojoWithTimeout(mojo);

        // Mojo loads blueprint
        RecordedRequest request = server.takeRequest(1, TimeUnit.MILLISECONDS);
        assertEquals(blueprintPath, request.getPath());
        assertEquals("GET", request.getMethod());

        // Then posts it to the server
        request = server.takeRequest(1, TimeUnit.MILLISECONDS);
        assertEquals("/v1/applications", request.getPath());
        assertEquals("POST", request.getMethod());
        assertEquals(YAML, new String(request.getBody()));

        // Subsequent steps are already tested by testPostsConfiguredBlueprintToServerAndSetsConfiguredProperty.
    }

    @Test
    public void testFailsIfServerUnreachable() throws Exception {
        DeployBlueprintMojo mojo = new DeployBlueprintMojo(
                new URL("http", "localhost", Networking.nextAvailablePort(34567), "/"),
                blueprintPath);
        try {
            mojo.execute();
            fail("Expected exception when running mojo with no server");
        } catch (MojoFailureException e) {
            // ignored
        }
    }

    @Test
    public void testFailsIfBlueprintUnreadable() throws Exception {
        server.play();
        String file = "/sdfjhsbfhd/dhbgjdfg/sdkfhsjydgfjsbdgcnv/x/bv";
        DeployBlueprintMojo mojo = new DeployBlueprintMojo(server.getUrl("/"), file);
        try {
            mojo.execute();
            fail("Expected exception when running mojo with nonsense file");
        } catch (MojoFailureException e) {
            // ignored
        }
    }

    @Test
    public void testBailsIfServerRespondsWithError() throws Exception {
        server.enqueue(newJsonResponse().setResponseCode(401).setBody("{\"message\": \"Unauthorized\"}"));
        server.play();
        DeployBlueprintMojo mojo = new DeployBlueprintMojo(server.getUrl("/"), blueprintPath);
        try {
            executeMojoWithTimeout(mojo);
            fail("Expected mojo to throw an exception when server responds 401");
        } catch (MojoFailureException e) {
            assertTrue("Expected exception message to contain 401 status code, was: " + e.getMessage(),
                    e.getMessage().contains("401"));
        }
    }

    @Test
    public void testDeployFailsIfAppStartTakesTooLong() throws Exception {
        server.enqueue(newJsonResponse().setBody(Jsonya.newInstance().put("entityId", APP_ID).toString()));
        server.enqueue(applicationStatusResponse("STARTING"));
        server.enqueue(applicationStatusResponse("STARTING"));
        server.enqueue(applicationStatusResponse("STARTING"));
        server.play();
        DeployBlueprintMojo mojo = new DeployBlueprintMojo(server.getUrl("/"), blueprintPath);
        mojo.setPollPeriod(1, TimeUnit.MILLISECONDS)
                .setTimeout(2, TimeUnit.MILLISECONDS);
        try {
            executeMojoWithTimeout(mojo);
            fail("Expected mojo to throw when app start takes too long");
        } catch (MojoFailureException e) {
            assertTrue("Expected exception message to contain 'should be running but is starting'. Was: " + e.getMessage(),
                    e.getMessage().contains("should be running but is starting"));
        }
    }

    @Test
    public void testBailsOutIfDeployedAppIsOnFire() throws Exception {
        server.enqueue(newJsonResponse().setBody(Jsonya.newInstance().put("entityId", APP_ID).toString()));
        server.enqueue(applicationStatusResponse("STARTING"));
        server.enqueue(applicationStatusResponse("ERROR"));
        server.play();
        DeployBlueprintMojo mojo = new DeployBlueprintMojo(server.getUrl("/"), blueprintPath, NEW_APP_PROPERTY);
        mojo.setPollPeriod(1, TimeUnit.MILLISECONDS);
        mojo.setNoStopAppOnDeployError();

        try {
            executeMojoWithTimeout(mojo);
        } catch (MojoFailureException e) {
            assertTrue("Expected exception message to contain 'should be running but is error', is: " + e.getMessage(),
                    e.getMessage().contains("should be running but is error"));
        }
    }

    @Test
    public void testFailsIfGivenNonsenseCharset() throws Exception {
        server.play();
        DeployBlueprintMojo mojo = new DeployBlueprintMojo(server.getUrl("/"), blueprintPath);
        mojo.setBlueprintEncoding("post-modern");
        try {
            mojo.execute();
            fail("Expected exception when running mojo with nonsense charset");
        } catch (MojoFailureException e) {
            // ignored
        }
    }

}
