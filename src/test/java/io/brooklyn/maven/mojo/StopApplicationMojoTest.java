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
package io.brooklyn.maven.mojo;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import io.brooklyn.maven.AbstractBrooklynMojoTest;

public class StopApplicationMojoTest extends AbstractBrooklynMojoTest {

    private static final String APPLICATION = "abcdef";

    @Test
    public void testAttemptsToStopApplication() throws Exception {
        server.enqueue(new MockResponse());
        server.play();

        StopApplicationMojo mojo = new StopApplicationMojo(server.getUrl("/"), APPLICATION);
        executeMojoWithTimeout(mojo);

        RecordedRequest request = server.takeRequest(1, TimeUnit.MILLISECONDS);
        final String expectedPath = "/v1/applications/" + APPLICATION + "/entities/" + APPLICATION + "/effectors/stop" +
                "?timeout=" + mojo.getTimeout().toMilliseconds();
        assertEquals(expectedPath, request.getPath());
        assertEquals("POST", request.getMethod());
        assertEquals(1, server.getRequestCount());
    }

}
