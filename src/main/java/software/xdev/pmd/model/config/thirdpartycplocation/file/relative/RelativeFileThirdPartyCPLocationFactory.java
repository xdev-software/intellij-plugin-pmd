package software.xdev.pmd.model.config.thirdpartycplocation.file.relative;

import java.nio.file.Path;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;

import software.xdev.pmd.config.state.project.thirdpartycp.FileThirdPartyCPLocationState;
import software.xdev.pmd.model.config.thirdpartycplocation.file.FileThirdPartyCPLocationFactory;


public class RelativeFileThirdPartyCPLocationFactory
	extends FileThirdPartyCPLocationFactory<RelativeFileThirdPartyCPLocation>
{
	private final Project project;
	
	public RelativeFileThirdPartyCPLocationFactory(final Project project)
	{
		super(project);
		this.project = project;
	}
	
	@Override
	public RelativeFileThirdPartyCPLocation fromPersisted(final FileThirdPartyCPLocationState state)
	{
		final String absolutePath = this.projectFilePaths.makeProjectRelativePathAbsolute(
			this.projectFilePaths.toSystemPath(state.location()));
		final Path path = this.checkFileExists(absolutePath);
		
		return new RelativeFileThirdPartyCPLocation(
			state.id(),
			this.pathToUrl(path),
			state.location(),
			absolutePath
		);
	}
	
	@Override
	public RelativeFileThirdPartyCPLocation fromUI(final Path uiState)
	{
		final Path projectPath = Objects.requireNonNull(
			this.determineProjectPath(),
			"Failed to determine project path");
		final Path relativePath = projectPath.relativize(uiState);
		
		return new RelativeFileThirdPartyCPLocation(
			this.newRandomId(),
			this.pathToUrl(uiState),
			this.projectFilePaths.toUnixPath(relativePath.toString()),
			uiState.toString()
		);
	}
	
	private @Nullable Path determineProjectPath()
	{
		final VirtualFile projectDir = ProjectUtil.guessProjectDir(this.project);
		return projectDir != null ? projectDir.toNioPath() : null;
	}
}
