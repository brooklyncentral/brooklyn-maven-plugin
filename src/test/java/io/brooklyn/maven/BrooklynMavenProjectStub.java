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

import java.util.Properties;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

public class BrooklynMavenProjectStub extends MavenProjectStub {

    public static final String MOCK_OUTPUT_DIRECTORY = "/output/directory";

    Properties properties = new Properties();

    @Override
    public Build getBuild() {
        return new MavenBuildStub();
    }

    // Superclass returns a new instance of properties each time it's called.
    @Override
    public Properties getProperties() {
        return properties;
    }

    private static class MavenBuildStub extends Build {
        @Override
        public String getDirectory() {
            return MOCK_OUTPUT_DIRECTORY;
        }
    }

}
