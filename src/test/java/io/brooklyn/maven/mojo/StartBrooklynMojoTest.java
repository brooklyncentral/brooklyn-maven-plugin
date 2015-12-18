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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Callables;

import io.brooklyn.maven.AbstractBrooklynMojoTest;
import io.brooklyn.maven.BrooklynMavenProjectStub;
import io.brooklyn.maven.fork.BrooklynForker;
import io.brooklyn.maven.fork.ForkOptions;
import io.brooklyn.maven.fork.ProjectDependencySupplier;
import io.brooklyn.maven.util.Context;

public class StartBrooklynMojoTest extends AbstractBrooklynMojoTest {

    private static final String DEPENDENCY_STRING = "/path/to/dependency";

    private static class EchoingForker implements BrooklynForker<ForkOptions> {
        @Override
        public Callable<ForkOptions> execute(ForkOptions config) throws MojoExecutionException {
            return Callables.returning(config);
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
        final EchoingForker forker = new EchoingForker();
        final String bindPort = "bindPort";
        final String bindAddress = "bindAddress";

        StartBrooklynMojo mojo = new StartBrooklynMojo(
                forker,
                dependencySupplier,
                bindAddress,
                bindPort,
                "mainClass",
                "classpathScope",
                mainUrlProperty);
        mojo.setProject(project);
        executeMojoWithTimeout(mojo);

        Object urlProperty = project.getProperties().get(mainUrlProperty);
        assertNotNull("Start goal did not set configured property " + mainUrlProperty, urlProperty);
        assertEquals(String.class, urlProperty.getClass());
        String serverUrl = urlProperty.toString();
        assertTrue("Server url did not contain configured bind address", serverUrl.contains(bindAddress));
        assertTrue("Server url did not contain configured bind port", serverUrl.contains(bindPort));

        // Goal should set a callable in the plugin context.
        Optional<Callable> context = Context.getForkedCallable(project, serverUrl);
        assertTrue("No callable set in project context for key: " + serverUrl, context.isPresent());

        Object result = context.get().call();
        assertNotNull("Callable did not return a value", result);
        assertTrue("Expected callable result to be an instance of " + ForkOptions.class.getName(), result instanceof ForkOptions);

        ForkOptions options = ForkOptions.class.cast(result);
        assertEquals("mainClass", options.mainClass());
        assertEquals(bindAddress, options.bindAddress());
        assertEquals(bindPort, options.bindPort());
        assertEquals(dependencySupplier.get(), options.classpath());
    }

}
