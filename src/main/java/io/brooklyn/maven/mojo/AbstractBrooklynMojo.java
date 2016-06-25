package io.brooklyn.maven.mojo;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.TimeUnit;

import org.apache.brooklyn.util.time.Duration;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.google.common.annotations.VisibleForTesting;

import io.brooklyn.maven.fork.BrooklynForker;

public abstract class AbstractBrooklynMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /**
     * Handles forks and cleaning them up.
     */
    @Component
    private BrooklynForker forker;

    /**
     * The duration mojos should wait for actions at Brooklyn to complete.
     */
    @Parameter(
            property = "brooklyn.timeout",
            defaultValue = "5")
    private Integer timeout;

    /**
     * The unit associated with {@link #timeout}.
     */
    @Parameter(
            property = "brooklyn.timeoutUnit",
            defaultValue = "MINUTES")
    private TimeUnit timeoutUnit;

    /**
     * Configure the goal to skip execution.
     */
    @Parameter(
            property = "skipTests",
            defaultValue = "false")
    private Boolean skipTests;

    /**
     * Configure the goal to skip execution.
     */
    @Parameter(
            property = "skipITs",
            defaultValue = "false")
    private Boolean skipITs;

    /**
     * Configure the goal to ignore {@link #skipTests}. Useful if your use of the plugin
     * is outside of the default pre- and post-integration-test phases.
     */
    @Parameter(
            property = "ignoreSkipTests",
            defaultValue = "false")
    private Boolean ignoreSkipTests;

    public AbstractBrooklynMojo() {
        super();
        this.timeout = 5;
        this.timeoutUnit = TimeUnit.SECONDS;
        this.skipTests = false;
        this.skipITs = false;
        this.ignoreSkipTests = false;
    }

    protected MavenProject getProject() {
        return project;
    }

    @VisibleForTesting
    AbstractBrooklynMojo setProject(MavenProject project) {
        this.project = project;
        return this;
    }

    protected BrooklynForker getForker() {
        return forker;
    }

    @VisibleForTesting
    void setForker(BrooklynForker forker) {
        this.forker = forker;
    }

    protected Duration getTimeout() {
        return Duration.of(timeout, timeoutUnit);
    }

    @VisibleForTesting
    AbstractBrooklynMojo setTimeout(int timeout, TimeUnit unit) {
        this.timeout = checkNotNull(timeout, "timeout");
        this.timeoutUnit = checkNotNull(unit, "unit");
        return this;
    }

    /**
     * @return true if either skipTests or skipITs is true and ignoreSkipTests is false.
     */
    protected boolean skipExecution() {
        return (skipTests || skipITs) && !ignoreSkipTests;
    }

    @VisibleForTesting
    AbstractBrooklynMojo setIgnoreSkipTests() {
        this.ignoreSkipTests = true;
        return this;
    }

    @VisibleForTesting
    AbstractBrooklynMojo setSkipITs() {
        this.skipITs = true;
        return this;
    }

    @VisibleForTesting
    AbstractBrooklynMojo setSkipTests() {
        this.skipTests = true;
        return this;
    }

    /**
     * The main goal logic.
     */
    abstract void doIt() throws MojoExecutionException, MojoFailureException;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skipExecution()) {
            getLog().info("Tests are skipped.");
        } else {
            doIt();
        }
    }

}
