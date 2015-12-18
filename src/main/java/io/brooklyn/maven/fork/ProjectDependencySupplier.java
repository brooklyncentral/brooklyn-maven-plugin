package io.brooklyn.maven.fork;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;

/**
 * Determines the classpath for a forked Brooklyn process by setting an
 * artifact filter on the project. It works but uses a restricted API.
 */
@Component(
        role = ProjectDependencySupplier.class,
        hint = "default")
public class ProjectDependencySupplier implements Supplier<List<Path>> {

    @Requirement
    private MavenProject project;

    // Would like to inject these too. This class would then not need to be referenced
    // by StartBrooklynMojo.
    private ArtifactRepository localRepository;
    private String scope;
    private boolean testOutputDirOnClasspath;
    private boolean outputDirOnClasspath;

    public ProjectDependencySupplier setLocalRepository(ArtifactRepository localRepository) {
        this.localRepository = localRepository;
        return this;
    }

    public ProjectDependencySupplier setOutputDirOnClasspath(boolean outputDirOnClasspath) {
        this.outputDirOnClasspath = outputDirOnClasspath;
        return this;
    }

    public ProjectDependencySupplier setScope(String scope) {
        this.scope = scope;
        return this;
    }

    public ProjectDependencySupplier setTestOutputDirOnClasspath(boolean testOutputDirOnClasspath) {
        this.testOutputDirOnClasspath = testOutputDirOnClasspath;
        return this;
    }

    /**
     * @return All artifacts in the configured scope.
     */
    @Override
    public List<Path> get() {
        checkNotNull(project, "project");
        checkNotNull(localRepository, "localRepository");
        checkNotNull(scope, "scope");

        ImmutableList.Builder<Path> urls = ImmutableList.builder();
        // Only include project directories if configured.
        if (Boolean.TRUE.equals(testOutputDirOnClasspath)) {
            String testOut = project.getBuild().getTestOutputDirectory();
            urls.add(Paths.get(testOut));
        }

        if (Boolean.TRUE.equals(outputDirOnClasspath)) {
            String outDir = project.getBuild().getOutputDirectory();
            urls.add(Paths.get(outDir));
        }

        // TODO: docs on setArtifactFilter say it MUST NOT BE USED.
        project.setArtifactFilter(new ScopeArtifactFilter(scope));
        Set<Artifact> artifacts = project.getArtifacts();
        final String repoBaseDir = localRepository.getBasedir();
        for (Artifact artifact : artifacts) {
            Path path = Paths.get(repoBaseDir, localRepository.pathOf(artifact));
            urls.add(path.toAbsolutePath());
        }

        return urls.build();

    }
}
