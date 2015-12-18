package io.brooklyn.maven.fork;

import java.net.URL;

import org.apache.maven.plugin.MojoExecutionException;

public interface BrooklynForker {

    /**
     * Fork a Brooklyn process with the given set of options.
     * @return A callable that can be waited on the for forked process' termination.
     * @throws MojoExecutionException if a process could not be forked.
     */
    URL execute(ForkOptions config) throws MojoExecutionException;

    /**
     * Stop all forked Brooklyn processes.
     */
    void cleanUp();

    /**
     * Stop a Brooklyn process.
     */
    void cleanUp(ShutdownOptions options);

}
