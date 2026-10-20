package software.xdev.pmd.maven.resolve.mirror;

import java.util.Optional;

import com.intellij.openapi.project.Project;

import software.xdev.pmd.util.ep.HasOrder;


public interface MavenMirrorUrlResolver extends HasOrder
{
	String DEFAULT_CENTRAL_REPOSITORY_URL = "https://repo.maven.apache.org/maven2/";
	
	Optional<String> resolve(Project project);
}
