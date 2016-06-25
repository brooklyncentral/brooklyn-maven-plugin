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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;

import io.brooklyn.maven.AbstractBrooklynMojoTest;
import io.brooklyn.maven.BrooklynMavenProjectStub;
import io.brooklyn.maven.fork.BrooklynForker;
import io.brooklyn.maven.fork.ForkOptions;
import io.brooklyn.maven.fork.ForkedServer;
import io.brooklyn.maven.fork.ProjectDependencySupplier;
import io.brooklyn.maven.fork.ShutdownOptions;

public class StartBrooklynMojoTest extends AbstractBrooklynMojoTest {

    private static final String DEPENDENCY_STRING = "/path/to/dependency";

    private static class RecordingForker implements BrooklynForker {
        ForkOptions options;

        @Override
        public ForkedServer execute(ForkOptions options) throws MojoExecutionException {
            try {
                this.options = options;
                Future<Integer> mockExitStatus = Futures.immediateFuture(0);
                return new ForkedServer(new URL("http", options.bindAddress(), options.bindPort()), mockExitStatus);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void cleanUp() {
        }

        @Override
        public void cleanUp(ShutdownOptions options) {
        }
    }

    private static class ConstantDependencySupplier extends ProjectDependencySupplier {
        @Override
        public List<Path> get() {
            return ImmutableList.of(Paths.get(DEPENDENCY_STRING));
        }
    }

    @Test
    public void testStartGoal() throws Exception {
        final MavenProjectStub project = new BrooklynMavenProjectStub();
        final String mainUrlProperty = "mainUrlProperty";
        final ConstantDependencySupplier dependencySupplier = new ConstantDependencySupplier();
        final RecordingForker forker = new RecordingForker();
        final String bindPort = "bindPort";
        final String bindAddress = "bindAddress";

        StartBrooklynMojo mojo = new StartBrooklynMojo(
                dependencySupplier,
                bindAddress,
                bindPort,
                "mainClass",
                "launchCommand",
                "classpathScope",
                mainUrlProperty);
        mojo.setProject(project);
        mojo.setForker(forker);
        // Test ignoreSkipTests at the same time.
        mojo.setIgnoreSkipTests();
        mojo.setSkipTests();
        executeMojoWithTimeout(mojo);

        Object urlProperty = project.getProperties().get(mainUrlProperty);
        assertNotNull("Start goal did not set configured property " + mainUrlProperty, urlProperty);
        assertEquals(String.class, urlProperty.getClass());
        String serverUrl = urlProperty.toString();
        assertTrue("Server url did not contain configured bind address", serverUrl.contains(bindAddress));
        assertTrue("Server url did not contain configured bind port", serverUrl.contains(bindPort));

        ForkOptions options = forker.options;
        assertNotNull("BrooklynForker class was not called", options);
        assertEquals("mainClass", options.mainClass());
        assertEquals("launchCommand", options.launchCommand());
        assertEquals(bindAddress, options.bindAddress());
        assertEquals(bindPort, options.bindPort());
        assertEquals(dependencySupplier.get(), options.classpath());
    }

    @Test
    public void testRespectsSkipTests() throws Exception {
        server.play();
        StartBrooklynMojo mojo = new StartBrooklynMojo();
        mojo.setSkipTests();
        mojo.execute();
        assertEquals("expected no requests to server when skipTests set", 0, server.getRequestCount());
    }

    @Test
    public void testRespectsSkipITs() throws Exception {
        server.play();
        StartBrooklynMojo mojo = new StartBrooklynMojo();
        mojo.setSkipITs();
        mojo.execute();
        assertEquals("expected no requests to server when skipITs set", 0, server.getRequestCount());
    }

}
