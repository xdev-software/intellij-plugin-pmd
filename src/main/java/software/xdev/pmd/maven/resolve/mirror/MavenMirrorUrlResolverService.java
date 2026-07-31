package software.xdev.pmd.maven.resolve.mirror;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;


public class MavenMirrorUrlResolverService
{
	private final ExtensionPointName<MavenMirrorUrlResolver> epMirrorUrlResolver =
		ExtensionPointName.create("software.xdev.pmd.mavenMirrorUrlResolver");
	
	private List<MavenMirrorUrlResolver> lastSeenExtensions;
	private List<MavenMirrorUrlResolver> cachedUrlResolvers;
	
	private List<MavenMirrorUrlResolver> orderedLangResolvers()
	{
		final List<MavenMirrorUrlResolver> extensions = this.epMirrorUrlResolver.getExtensionList();
		if(this.cachedUrlResolvers == null || extensions != this.lastSeenExtensions)
		{
			this.cachedUrlResolvers = extensions
				.stream()
				.sorted(Comparator.comparingInt(MavenMirrorUrlResolver::order))
				.toList();
			this.lastSeenExtensions = extensions;
		}
		return this.cachedUrlResolvers;
	}
	
	public Optional<String> resolve(final Project project)
	{
		return this.orderedLangResolvers()
			.stream()
			.map(r -> r.resolve(project))
			.filter(Optional::isPresent)
			.map(Optional::orElseThrow)
			.findFirst();
	}
}
