package io.brooklyn.maven.fork;

import java.nio.file.Path;
import java.util.List;
import javax.annotation.Nullable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ForkOptions {

    ForkOptions() {}

    public static Builder builder() {
        return new AutoValue_ForkOptions.Builder();
    }

    // General process options
    public abstract Path workDir();
    public abstract List<String> javaOptions();
    public abstract List<String> additionalArguments();
    public abstract List<Path> classpath();

    // Brooklyn options
    public abstract String mainClass();
    public abstract String launchCommand();
    public abstract String bindAddress();
    public abstract String bindPort();
    @Nullable public abstract String username();
    @Nullable public abstract String password();

    @AutoValue.Builder
    public abstract static class Builder {
        // General process options
        public abstract Builder javaOptions(List<String> options);
        public abstract Builder workDir(Path dir);
        public abstract Builder additionalArguments(List<String> arguments);
        public abstract Builder classpath(List<Path> classpath);

        // Brooklyn options
        public abstract Builder launchCommand(String command);
        public abstract Builder mainClass(String clazz);
        public abstract Builder bindAddress(String address);
        public abstract Builder bindPort(String port);
        public abstract Builder username(@Nullable String username);
        public abstract Builder password(@Nullable String port);

        public abstract ForkOptions build();
    }

}
