package io.brooklyn.maven.fork;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.brooklyn.rest.client.BrooklynApi;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.utils.cli.CommandLineCallable;
import org.apache.maven.shared.utils.cli.CommandLineUtils;
import org.apache.maven.shared.utils.cli.Commandline;
import org.apache.maven.shared.utils.cli.DefaultConsumer;
import org.apache.maven.shared.utils.cli.StreamConsumer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.Logger;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.io.Resources;

@Singleton
@Component(
        role = BrooklynForker.class,
        hint = "default")
public class BasicBrooklynForker implements BrooklynForker {

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<String, ServerRecord> forks = Maps.newHashMap();
    private final Object forksLock = new Object[0];

    private Logger logger;

    @Inject
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void cleanUp() {
        // Could make this asynchronous but expect it will be rare to wait on more than one server.
        synchronized (forksLock) {
            for (Iterator<ServerRecord> it = forks.values().iterator(); it.hasNext(); ) {
                ServerRecord entry = it.next();
                try {
                    doCleanUp(entry.shutdownOptions, entry);
                } finally {
                    it.remove();
                }
            }
        }
        executorService.shutdown();
    }

    @Override
    public void cleanUp(ShutdownOptions options) {
        synchronized (forksLock) {
            ServerRecord record = forks.remove(options.server().toString());
            doCleanUp(options, record);
        }
    }

    private void doCleanUp(ShutdownOptions options, ServerRecord record) {
        logger.info("Stopping server at " + options.server());
        logger.debug("Stop options: " +
                "timeout=" + options.timeout() +
                ", stopApps=" + options.stopAllApplications() +
                ", force=" + options.forceShutdownOnError());
        BrooklynApi api;
        if (options.username() == null || options.password() == null) {
            api = BrooklynApi.newInstance(options.server().toString());
        } else {
            api = BrooklynApi.newInstance(options.server().toString(), options.username(), options.password());
        }
        api.getServerApi().shutdown(
                options.stopAllApplications(),
                options.forceShutdownOnError(),
                options.timeout(),
                options.timeout(),
                options.timeout(),
                null /* argument deprecated */);

        // Could be null if the start-server goal was not used.
        if (record != null) {
            try {
                // Waits for the forked process to exit.
                logger.debug("Waiting for forked process to complete");
                // Status is -1 if it timed out.
                int status = record.forkedServer.getExitCode(30, TimeUnit.SECONDS);
                String statusMessage = status >= 0 ? " with exit status " + status : ", unable to determine exit status";
                logger.debug("Forked process complete" + statusMessage);
            } catch (Exception e) {
                logger.warn("Exception waiting for server at " + options.server() + " to exit", e);
            }
        } else {
            logger.debug("Cannot wait for server to exit: no callable context.");
        }
    }

    @Override
    public ForkedServer execute(ForkOptions options) throws MojoExecutionException {
        Commandline cl = buildCommandLine(options);
        // DefaultConsumer simply calls System.out.println.
        StreamConsumer sysout = new DefaultConsumer();
        StreamConsumer syserr = sysout;
        logger.debug("Executing: " + cl);
        try {
            // TODO: should inject whether server is http or https.
            final URL serverUrl = new URL("http://" + options.bindAddress() + ":" + options.bindPort());
            // First null: no stdin. Second: no runnable after termination.
            // Zero: wait forever for termination.
            final CommandLineCallable callable = CommandLineUtils.executeCommandLineAsCallable(
                    cl, null, sysout, syserr, 0, null);
            final Future<Integer> future = executorService.submit(callable);
            final ForkedServer forkedServer = new ForkedServer(serverUrl, future);

            // Record flags so server can be killed cleanly later.
            final ShutdownOptions shutdownOptions = ShutdownOptions.builder()
                    .server(serverUrl)
                    .username(options.username())
                    .password(options.password())
                    .build();
            synchronized (forksLock) {
                forks.put(serverUrl.toString(), new ServerRecord(shutdownOptions, forkedServer));
            }
            return forkedServer;
        } catch (Exception e) {
            throw new MojoExecutionException("Error forking server", e);
        }
    }

    private Commandline buildCommandLine(ForkOptions options) throws MojoExecutionException {
        Commandline cl = new Commandline();
        cl.addSystemEnvironment();
        // todo: inject other environment variables
        cl.setWorkingDirectory(createOutputDirectory(options.workDir()));
        // todo: use same java version as maven?
        cl.setExecutable("java");
        for (String javaOption : options.javaOptions()) {
            cl.createArg().setValue(javaOption);
        }
        cl.createArg().setValue("-classpath");
        cl.createArg().setValue(buildClasspath(options));
        cl.createArg().setValue(options.mainClass());
        cl.createArg().setValue(options.launchCommand());
        cl.createArg().setValue("--bindAddress");
        cl.createArg().setValue(options.bindAddress());
        cl.createArg().setValue("--port");
        cl.createArg().setValue(options.bindPort());
        for (String argument : options.additionalArguments()) {
            cl.createArg().setValue(argument);
        }
        return cl;
    }

    /**
     * Creates and returns the path to a directory in the project's build directory.
     */
    private String createOutputDirectory(Path workingDir) throws MojoExecutionException {
        Path confDir = workingDir.resolve("conf");
        boolean createdOutputDir = confDir.toFile().mkdirs();
        if (!createdOutputDir) {
            throw new MojoExecutionException("Could not create configuration directory: " + confDir);
        }
        try {
            String logback = Resources.asCharSource(getClass().getResource("/logback.xml"), Charsets.UTF_8).read();
            Files.write(logback, confDir.resolve("logback.xml").toFile(), Charsets.UTF_8);
        } catch (IOException e) {
            throw new MojoExecutionException("Error writing logback configuration", e);
        }
        return workingDir.toString();
    }

    private String buildClasspath(ForkOptions options) {
        final String separator = System.getProperty("path.separator");
        // Head of the classpath is the directory containing logback.xml
        final Path conf = options.workDir().resolve("conf").toAbsolutePath();
        final StringBuilder classpath = new StringBuilder(conf.toString());
        for (Path path : options.classpath()) {
            classpath.append(separator).append(path);
        }
        return classpath.toString();
    }

    private static class ServerRecord {
        final ShutdownOptions shutdownOptions;
        final ForkedServer forkedServer;
        private ServerRecord(ShutdownOptions shutdownOptions, ForkedServer forkedServer) {
            this.shutdownOptions = shutdownOptions;
            this.forkedServer = forkedServer;
        }
    }

}
