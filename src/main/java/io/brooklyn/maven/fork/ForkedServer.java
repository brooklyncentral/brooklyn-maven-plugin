package io.brooklyn.maven.fork;

import java.net.URL;
import java.util.concurrent.Future;

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

}
