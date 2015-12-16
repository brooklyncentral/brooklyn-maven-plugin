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

public class StopBrooklynMojoTest extends AbstractBrooklynMojoTest {

    @Test
    public void testPostsShutdownToServer() throws Exception {
        server.enqueue(new MockResponse());
        server.play();

        StopBrooklynMojo mojo = new StopBrooklynMojo(server.getUrl("/"));
        mojo.setPollPeriod(1, TimeUnit.MILLISECONDS);
        executeMojoWithTimeout(mojo);

        RecordedRequest r = server.takeRequest(1, TimeUnit.MILLISECONDS);
        assertEquals("/v1/server/shutdown", r.getPath());
        assertEquals("POST", r.getMethod());
    }

}
