package io.brooklyn.maven.fork;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.project.MavenProject;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;

/**
 * Determines the classpath for a forked Brooklyn process by setting an
 * artifact filter on the project. It works but uses a restricted API.
 */
public class ProjectDependencySupplier implements Supplier<List<Path>> {

    private final MavenProject project;
    private final ArtifactRepository localRepository;
    private final String scope;

    public ProjectDependencySupplier(
            MavenProject project, ArtifactRepository localRepository, String scope) {
        this.project = project;
        this.localRepository = localRepository;
        this.scope = scope;
    }

    /**
     * @return All artifacts in the configured scope.
     */
    @Override
    public List<Path> get() {
        // TODO: docs on setArtifactFilter say it MUST NOT BE USED.
        project.setArtifactFilter(new ScopeArtifactFilter(scope));
        Set<Artifact> artifacts = project.getArtifacts();
        final String repoBaseDir = localRepository.getBasedir();
        final ImmutableList.Builder<Path> urls = ImmutableList.builder();
        for (Artifact artifact : artifacts) {
            Path path = Paths.get(repoBaseDir, localRepository.pathOf(artifact));
            urls.add(path.toAbsolutePath());
        }
        return urls.build();
    }
}
