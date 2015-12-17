package io.brooklyn.maven.fork;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.utils.cli.CommandLineCallable;
import org.apache.maven.shared.utils.cli.CommandLineException;
import org.apache.maven.shared.utils.cli.CommandLineUtils;
import org.apache.maven.shared.utils.cli.Commandline;
import org.apache.maven.shared.utils.cli.DefaultConsumer;
import org.apache.maven.shared.utils.cli.StreamConsumer;
import org.codehaus.plexus.component.annotations.Component;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;

@Component(
        role = BrooklynForker.class,
        hint = "default")
public class BasicBrooklynForker implements BrooklynForker<Integer> {

    @Override
    public CommandLineCallable execute(ForkOptions options) throws MojoExecutionException {
        Commandline cl = buildCommandLine(options);
        // DefaultConsumer simply calls System.out.println.
        StreamConsumer sysout = new DefaultConsumer();
        StreamConsumer syserr = sysout;
        // todo would like to inject but surprising to user to have to give the argument to the start goal.
        final int shutdownTimeout = 60;
        // getLog().debug("Executing: " + cl);
        try {
            // First null: no stdin. Second: no runnable after termination.
            return CommandLineUtils.executeCommandLineAsCallable(cl, null, sysout, syserr, shutdownTimeout, null);
        } catch (CommandLineException e) {
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
        cl.createArg().setValue("-classpath");
        cl.createArg().setValue(buildClasspath(options));
        cl.createArg().setValue(options.mainClass());
        cl.createArg().setValue("launch");
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
        confDir.toFile().mkdirs();
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
}
