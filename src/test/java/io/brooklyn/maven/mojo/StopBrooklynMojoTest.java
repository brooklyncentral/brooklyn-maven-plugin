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

import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Test;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import io.brooklyn.maven.AbstractBrooklynMojoTest;
import io.brooklyn.maven.fork.BasicBrooklynForker;

public class StopBrooklynMojoTest extends AbstractBrooklynMojoTest {

    @Test
    public void testPostsShutdownToServer() throws Exception {
        server.enqueue(new MockResponse());
        server.play();

        final BasicBrooklynForker forker = new BasicBrooklynForker();
        forker.setLogger(new ConsoleLogger());
        StopBrooklynMojo mojo = new StopBrooklynMojo(server.getUrl("/"));
        mojo.setForker(forker);
        mojo.setPollPeriod(1, TimeUnit.MILLISECONDS);
        // Test ignoreSkipTests at the same time.
        mojo.setIgnoreSkipTests();
        mojo.setSkipTests();
        executeMojoWithTimeout(mojo);

        RecordedRequest r = server.takeRequest(1, TimeUnit.MILLISECONDS);
        assertEquals("/v1/server/shutdown", r.getPath());
        assertEquals("POST", r.getMethod());
    }

    @Test
    public void testRespectsSkipTests() throws Exception {
        server.play();
        StopBrooklynMojo mojo = new StopBrooklynMojo();
        mojo.setSkipTests();
        mojo.execute();
        assertEquals("expected no requests to server when skipTests set", 0, server.getRequestCount());
    }

    @Test
    public void testRespectsSkipITs() throws Exception {
        server.play();
        StopBrooklynMojo mojo = new StopBrooklynMojo();
        mojo.setSkipITs();
        mojo.execute();
        assertEquals("expected no requests to server when skipITs set", 0, server.getRequestCount());
    }

}
