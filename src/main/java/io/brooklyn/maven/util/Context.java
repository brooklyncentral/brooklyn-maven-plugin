package io.brooklyn.maven.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.Callable;
import javax.annotation.Nullable;

import org.apache.maven.project.MavenProject;

import com.google.common.base.Optional;

public class Context {

    private static final String BROOKLYN_PROCESS_CONTEXT = "BrooklynCommandLineCallable";

    private Context() {}

    public static Optional<Callable> getForkedCallable(@Nullable MavenProject project, String url) {
        if (project != null) {
            Object context = project.getContextValue(getCallableContextKey(url));
            if (context != null && context instanceof Callable) {
                return Optional.of(Callable.class.cast(context));
            }
        }
        return Optional.absent();
    }

    public static void setForkedCallable(MavenProject project, String url, Callable<?> callable) {
        checkNotNull(project, "project");
        checkNotNull(url, "url");
        checkNotNull(callable, "callable");
        project.setContextValue(getCallableContextKey(url), callable);
    }

    public static Optional<Callable> unsetForkedCallable(@Nullable MavenProject project, String url) {
        Optional<Callable> callable = getForkedCallable(project, url);
        if (project != null && callable.isPresent()) {
            project.setContextValue(getCallableContextKey(url), null);
        }
        return callable;
    }

    private static String getCallableContextKey(String url) {
        return BROOKLYN_PROCESS_CONTEXT + "-" + url;
    }

}
