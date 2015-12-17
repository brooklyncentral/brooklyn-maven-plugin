package io.brooklyn.maven.fork;

import java.util.concurrent.Callable;

import org.apache.maven.plugin.MojoExecutionException;

public interface BrooklynForker<T> {

    /**
     * Fork a Brooklyn process with the given set of options.
     * @return A callable that can be waited on the for forked process' termination.
     * @throws MojoExecutionException if a process could not be forked.
     */
    Callable<T> execute(ForkOptions config) throws MojoExecutionException;

}
