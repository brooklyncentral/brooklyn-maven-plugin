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

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.apache.maven.it.Verifier;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import brooklyn.util.collections.Jsonya;

// Could use plugin-testing-harness' MojoRule when https://jira.codehaus.org/browse/MPLUGINTESTING-45
// is resolved.
public class VerifyGoalsIntegrationTest extends AbstractBrooklynMojoTest {

    private static final String APPLICATION = "fedcba";

    @Rule
    public TestResources resources = new TestResources();

    @Test
    public void testDeployGoal() throws Exception {
        // Just enough of a task summary to work.
        server.enqueue(newJsonResponse().setBody(Jsonya.newInstance().put("entityId", APPLICATION).toString()));
        server.enqueue(applicationStatusResponse("RUNNING"));
        server.play();

        File dir = resources.getBasedir("test-deploy-goal");
        Verifier verifier = new Verifier(dir.getAbsolutePath());
        verifier.setMavenDebug(true);
        verifier.executeGoal("pre-integration-test", ImmutableMap.of(
                "server", server.getUrl("/").toString()));
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("test-deploy-goal result: " + APPLICATION);
    }

    @Test
    public void testSensorGoal() throws Exception {
        final String sensorVal = "Eric";
        server.enqueue(newJsonResponse()
                .setBody(Jsonya.newInstance().put(APPLICATION, sensorVal).toString()));
        server.play();

        File dir = resources.getBasedir("test-sensor-goal");
        Verifier verifier = new Verifier(dir.getAbsolutePath());
        verifier.executeGoal("pre-integration-test", ImmutableMap.of(
                "server", server.getUrl("/").toString(),
                "appId", APPLICATION));

        RecordedRequest request = server.takeRequest(1, TimeUnit.MILLISECONDS);
        assertTrue(request.getPath().startsWith(String.format("/v1/applications/%s/entities/%s/descendants/sensor/",
                APPLICATION, APPLICATION)));
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("test-sensor-goal result: " + sensorVal);
    }

    @Test
    public void testStopGoal() throws Exception {
        server.enqueue(new MockResponse());
        server.play();

        File dir = resources.getBasedir("test-stop-goal");
        Verifier verifier = new Verifier(dir.getAbsolutePath());
        verifier.executeGoal("post-integration-test", ImmutableMap.of(
                "server", server.getUrl("/").toString(),
                "appId", APPLICATION));
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("Stopping application " + APPLICATION);
    }

    @Test
    public void testWholeCaboodle() throws Exception {
        final String sensorVal = "Eric";
        server.enqueue(newJsonResponse().setBody(Jsonya.newInstance().put("entityId", APPLICATION).toString()));
        server.enqueue(applicationStatusResponse("RUNNING"));
        server.enqueue(newJsonResponse()
                .setBody(Jsonya.newInstance().put(APPLICATION, sensorVal).toString()));
        server.enqueue(new MockResponse());
        server.play();

        File dir = resources.getBasedir("example-app");
        Verifier verifier = new Verifier(dir.getAbsolutePath());
        verifier.getSystemProperties().put("server", server.getUrl("/").toString());
        verifier.executeGoals(ImmutableList.of("post-integration-test"));
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("Application: " + APPLICATION);
        verifier.verifyTextInLog("Sensor value: " + sensorVal);
        assertEquals(4, server.getRequestCount());
    }

}
