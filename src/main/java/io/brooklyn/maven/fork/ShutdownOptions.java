package io.brooklyn.maven.fork;

import java.net.URL;
import javax.annotation.Nullable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ShutdownOptions {

    ShutdownOptions() {}

    public static Builder builder() {
        return new AutoValue_ShutdownOptions.Builder()
                .timeout("0")
                .stopAllApplications(true)
                .forceShutdownOnError(true)
                ;
    }

    public abstract URL server();
    public abstract boolean stopAllApplications();
    public abstract boolean forceShutdownOnError();
    public abstract String timeout();
    @Nullable public abstract String username();
    @Nullable public abstract String password();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder server(URL server);
        public abstract Builder stopAllApplications(boolean stopAllApplications);
        public abstract Builder forceShutdownOnError(boolean forceShutdownOnError);
        public abstract Builder timeout(String timeout);
        public abstract Builder username(@Nullable String username);
        public abstract Builder password(@Nullable String port);

        public abstract ShutdownOptions build();
    }

}
