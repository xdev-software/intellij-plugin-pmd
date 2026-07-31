package software.xdev.pmd.maven.resolve.mirror;

import java.util.Optional;

import com.intellij.openapi.project.Project;


public interface MavenMirrorUrlResolver
{
	String DEFAULT_CENTRAL_REPOSITORY_URL = "https://repo.maven.apache.org/maven2/";
	
	default int order()
	{
		return 1000;
	}
	
	Optional<String> resolve(Project project);
}
