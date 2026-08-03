package software.xdev.pmd.model.config.thirdpartycplocation.file.absolute;

import java.nio.file.Path;

import com.intellij.openapi.project.Project;

import software.xdev.pmd.config.state.project.thirdpartycp.FileThirdPartyCPLocationState;
import software.xdev.pmd.model.config.thirdpartycplocation.file.FileThirdPartyCPLocationFactory;


public class AbsoluteFileThirdPartyCPLocationFactory
	extends FileThirdPartyCPLocationFactory<AbsoluteFileThirdPartyCPLocation>
{
	public AbsoluteFileThirdPartyCPLocationFactory(final Project project)
	{
		super(project);
	}
	
	@Override
	public AbsoluteFileThirdPartyCPLocation fromPersisted(final FileThirdPartyCPLocationState state)
	{
		final String absolutePath = this.projectFilePaths.toSystemPath(state.location());
		final Path path = this.checkFileExists(absolutePath);
		
		return new AbsoluteFileThirdPartyCPLocation(
			state.id(),
			this.pathToUrl(path),
			state.location(),
			absolutePath
		);
	}
	
	@Override
	public AbsoluteFileThirdPartyCPLocation fromUI(final Path uiState)
	{
		final String pathStr = uiState.toString();
		return new AbsoluteFileThirdPartyCPLocation(
			this.newRandomId(),
			this.pathToUrl(uiState),
			this.projectFilePaths.toUnixPath(pathStr),
			pathStr
		);
	}
}
