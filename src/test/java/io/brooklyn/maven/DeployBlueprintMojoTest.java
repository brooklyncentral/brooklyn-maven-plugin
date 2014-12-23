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
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.junit.Before;
import org.junit.Ignore;
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
        RecordedRequest request = server.takeRequest();
        assertEquals("/v1/applications", request.getPath());
        assertEquals("POST", request.getMethod());
        assertEquals(YAML, new String(request.getBody()));

        // Mojo waits for blueprint to be running
        request = server.takeRequest();
        assertEquals("/v1/applications/" + APP_ID, request.getPath());
        assertEquals("GET", request.getMethod());
        request = server.takeRequest();
        assertEquals("/v1/applications/" + APP_ID, request.getPath());
        assertEquals("GET", request.getMethod());

        // No more requests and the property was set
        assertEquals(3, server.getRequestCount());

        // And the property was set on the project
        assertEquals("Property " + NEW_APP_PROPERTY + " was not set on the Maven project",
                APP_ID, project.getProperties().getProperty(NEW_APP_PROPERTY));
    }

    @Ignore("unimplemented")
    @Test
    public void testLoadsBlueprintFromUrl() throws Exception {
        // Pretending to be both the server hosting the blueprint and Brooklyn.
        server.enqueue(new MockResponse().setBody(YAML));
        server.enqueue(newJsonResponse().setBody(Jsonya.newInstance().put("entityId", APP_ID).toString()));
        server.enqueue(applicationStatusResponse("RUNNING"));
        server.play();

        final String blueprintUrl = server.getUrl("/a/path/to/a/blueprint.yaml").toString();
        DeployBlueprintMojo mojo = new DeployBlueprintMojo(server.getUrl("/"), blueprintUrl, NEW_APP_PROPERTY);
        mojo.setPollPeriod(1, TimeUnit.MILLISECONDS);
        executeMojoWithTimeout(mojo);

        // Mojo loads blueprint
        RecordedRequest request = server.takeRequest();
        assertEquals(blueprintUrl, request.getPath());
        assertEquals("GET", request.getMethod());

        // Then posts it to the server
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
    @Ignore("Unimplemented")
    public void testTimeouts() {

    }

}
