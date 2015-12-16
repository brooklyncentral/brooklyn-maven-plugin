package io.brooklyn.maven.util;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.cli.CommandLineCallable;

import com.google.common.base.Optional;

public class Context {

    private static final String BROOKLYN_PROCESS_CONTEXT = "BrooklynCommandLineCallable";

    private Context() {}

    public static Optional<CommandLineCallable> getForkedCallable(@Nullable MavenProject project, String url) {
        if (project != null) {
            Object context = project.getContextValue(getCallableContextKey(url));
            if (context != null && context instanceof CommandLineCallable) {
                return Optional.of(CommandLineCallable.class.cast(context));
            }
        }
        return Optional.absent();
    }

    public static void setForkedCallable(MavenProject project, String url, CommandLineCallable callable) {
        checkNotNull(project, "project");
        checkNotNull(url, "url");
        checkNotNull(callable, "callable");
        project.setContextValue(getCallableContextKey(url), callable);
    }

    public static Optional<CommandLineCallable> unsetForkedCallable(@Nullable MavenProject project, String url) {
        Optional<CommandLineCallable> callable = getForkedCallable(project, url);
        if (project != null && callable.isPresent()) {
            project.setContextValue(getCallableContextKey(url), null);
        }
        return callable;
    }

    private static String getCallableContextKey(String url) {
        return BROOKLYN_PROCESS_CONTEXT + "-" + url;
    }

}
