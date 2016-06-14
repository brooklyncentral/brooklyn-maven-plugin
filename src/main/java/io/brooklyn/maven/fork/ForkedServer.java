package io.brooklyn.maven.fork;

import java.net.URL;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.brooklyn.util.exceptions.Exceptions;

public class ForkedServer {

    private final URL server;
    private final Future<Integer> exitCode;

    public ForkedServer(URL server, Future<Integer> exitCode) {
        this.server = server;
        this.exitCode = exitCode;
    }

    public URL getServer() {
        return server;
    }

    public boolean hasExited() {
        return exitCode.isDone();
    }

    public int getExitCode() {
        try {
            return exitCode.get();
        } catch (Exception e) {
            throw Exceptions.propagate(e);
        }
    }

    /**
     * Wait up to timeout for the exit status of the forked process.
     * Returns -1 if it could not be learned in time.
     */
    public int getExitCode(long timeout, TimeUnit unit) {
        try {
            return exitCode.get(timeout, unit);
        } catch (TimeoutException e) {
            return -1;
        } catch (Exception e) {
            throw Exceptions.propagate(e);
        }
    }

}
