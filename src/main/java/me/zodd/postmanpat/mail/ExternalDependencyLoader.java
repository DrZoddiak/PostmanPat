package me.zodd.postmanpat.mail;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

import java.util.List;
import java.util.stream.Stream;

@SuppressWarnings("UnstableApiUsage")
public class ExternalDependencyLoader implements PluginLoader {

    private final List<Dependency> artifacts = Stream.of(
                    "org.jetbrains.kotlin:kotlin-stdlib:2.0.20",
                    "org.spongepowered:configurate-hocon:4.1.2",
                    "org.spongepowered:configurate-extra-kotlin:4.1.2"
            )
            .map(DefaultArtifact::new)
            .map(a -> new Dependency(a, null))
            .toList();

    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {
        var resolver = new MavenLibraryResolver();
        // Mirror Repo for MavenCentral
        String mirrorURL = "http://129.153.81.179:8080/releases";
        resolver.addRepository(new RemoteRepository.Builder("mavenCentralMirror", "default", mirrorURL).build());
        artifacts.forEach(resolver::addDependency);
        classpathBuilder.addLibrary(resolver);
    }
}