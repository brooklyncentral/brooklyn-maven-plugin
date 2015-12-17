package io.brooklyn.maven.mojo;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.TimeUnit;

import org.apache.brooklyn.util.time.Duration;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.google.common.annotations.VisibleForTesting;

public abstract class AbstractBrooklynMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

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

    public AbstractBrooklynMojo() {
        super();
        this.timeout = 5;
        this.timeoutUnit = TimeUnit.SECONDS;
    }

    protected MavenProject getProject() {
        return project;
    }

    @VisibleForTesting
    AbstractBrooklynMojo setProject(MavenProject project) {
        this.project = project;
        return this;
    }

    protected Duration getTimeout() {
        return Duration.of(timeout, timeoutUnit);
    }

    AbstractBrooklynMojo setTimeout(int timeout, TimeUnit unit) {
        this.timeout = checkNotNull(timeout, "timeout");
        this.timeoutUnit = checkNotNull(unit, "unit");
        return this;
    }

}
