package software.xdev.pmd.maven.resolve.mirror;

import java.util.Optional;

import com.intellij.openapi.project.Project;

import software.xdev.pmd.util.ep.CachedOrderedExtensionPointContainer;


public class MavenMirrorUrlResolverService
{
	private final CachedOrderedExtensionPointContainer<MavenMirrorUrlResolver> container =
		new CachedOrderedExtensionPointContainer<>("mavenMirrorUrlResolver");
	
	public Optional<String> resolve(final Project project)
	{
		return this.container.orderedEps()
			.stream()
			.map(r -> r.resolve(project))
			.filter(Optional::isPresent)
			.map(Optional::orElseThrow)
			.findFirst();
	}
}
