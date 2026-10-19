package software.xdev.pmd.maven.ideamaven;

import java.util.Optional;

import org.jetbrains.idea.maven.utils.MavenEelUtil;
import org.jetbrains.idea.maven.utils.MavenUtil;

import com.intellij.openapi.project.Project;

import software.xdev.pmd.maven.resolve.mirror.MavenMirrorUrlResolver;


public class IDEAMavenMirrorUrlResolver implements MavenMirrorUrlResolver
{
	@Override
	public Optional<String> resolve(final Project project)
	{
		return Optional.ofNullable(MavenEelUtil.resolveUserSettingsPathBlocking(null, project))
			.map(path -> MavenUtil.INSTANCE.getMirroredUrl(path, DEFAULT_CENTRAL_REPOSITORY_URL, "central"))
			.filter(s -> !DEFAULT_CENTRAL_REPOSITORY_URL.equals(s));
	}
}
