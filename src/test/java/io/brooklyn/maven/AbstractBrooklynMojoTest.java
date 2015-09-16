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

import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.brooklyn.util.collections.Jsonya;
import org.apache.maven.plugin.AbstractMojo;
import org.junit.After;
import org.junit.Before;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

public abstract class AbstractBrooklynMojoTest {

    /**
     * Set as a system property to configure the time waited by
     * {@link #executeMojoWithTimeout(AbstractMojo)}.
     */
    private static final String TIMEOUT_PROPERTY = "brooklyn.test.timeout";

    MockWebServer server;

    @Before
    public void newMockWebServer() {
        server = new MockWebServer();
    }

    @After
    public void shutDownServer() throws Exception {
        if (server != null) server.shutdown();
    }

    /**
     * Executes the given mojo and fails if it does not succeed in a timely manner.
     * The timeout can be injected by setting {@link #TIMEOUT_PROPERTY} as a system
     * property.
     *
     * @see io.brooklyn.maven.AbstractInvokeBrooklynMojo#setPollPeriod(int, TimeUnit)
     */
    protected void executeMojoWithTimeout(AbstractMojo mojo) throws Exception {
        String configuredTimeout = System.getProperty(TIMEOUT_PROPERTY);
        Integer timeout = configuredTimeout != null
                ? Integer.valueOf(configuredTimeout)
                : 2;

        // The timeout is overkill on a normal machine but plausible on Travis, etc.
        executeMojoWithTimeout(mojo, timeout, TimeUnit.SECONDS);
    }

    /**
     * Executes the given mojo and fails if it does not succeed within the given period.
     * @see io.brooklyn.maven.AbstractInvokeBrooklynMojo#setPollPeriod(int, TimeUnit)
     */
    protected void executeMojoWithTimeout(final AbstractMojo mojo, int timeout, TimeUnit unit) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Exception> exception = new AtomicReference<Exception>();
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    mojo.execute();
                } catch (Exception e) {
                    exception.set(e);
                } finally {
                    latch.countDown();
                }
            }
        };
        t.start();
        boolean threadComplete = latch.await(timeout, unit);
        if (exception.get() != null) {
            if (t.isAlive()) t.interrupt();
            throw exception.get();
        } else if (!threadComplete) {
            t.interrupt();
            fail(mojo + " incomplete after " + timeout + " " + unit.name().toLowerCase());
        }
    }

    /** @return a response whose Content-Type header is application/json. */
    protected MockResponse newJsonResponse() {
        return new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    }

    protected MockResponse applicationStatusResponse(String status) {
        String body = Jsonya.newInstance()
                .put("status", status)
                .at("spec", "locations").list().add("localhost")
                .root()
                .toString();
        return newJsonResponse()
                .setBody(body);
    }

}
