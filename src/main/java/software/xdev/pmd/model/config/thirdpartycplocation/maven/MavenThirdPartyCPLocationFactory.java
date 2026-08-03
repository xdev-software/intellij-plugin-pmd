package software.xdev.pmd.model.config.thirdpartycplocation.maven;

import java.nio.file.Path;

import com.intellij.openapi.project.Project;

import software.xdev.pmd.config.state.project.thirdpartycp.MavenThirdPartyCPLocationState;
import software.xdev.pmd.maven.MavenId;
import software.xdev.pmd.maven.resolve.MavenArtifactResolver;
import software.xdev.pmd.model.config.thirdpartycplocation.ThirdPartyCPLocationFactory;


public class MavenThirdPartyCPLocationFactory
	extends ThirdPartyCPLocationFactory<MavenThirdPartyCPLocation, MavenThirdPartyCPLocationState, MavenId>
{
	private final Project project;
	
	public MavenThirdPartyCPLocationFactory(final Project project)
	{
		this.project = project;
	}
	
	@Override
	public MavenThirdPartyCPLocation fromPersisted(final MavenThirdPartyCPLocationState state)
	{
		final MavenId mavenId = new MavenId(state.groupId(), state.artifactId(), state.version());
		final Path resolvedPath = this.resolveMavenArtifactId(mavenId);
		
		return new MavenThirdPartyCPLocation(
			state.id(),
			this.pathToUrl(resolvedPath),
			mavenId
		);
	}
	
	@Override
	public MavenThirdPartyCPLocation fromUI(final MavenId uiState)
	{
		final Path resolvedPath = this.resolveMavenArtifactId(uiState);
		
		return new MavenThirdPartyCPLocation(
			this.newRandomId(),
			this.pathToUrl(resolvedPath),
			uiState
		);
	}
	
	protected Path resolveMavenArtifactId(final MavenId mavenId)
	{
		return this.project.getService(MavenArtifactResolver.class).ensureResolved(mavenId);
	}
}
