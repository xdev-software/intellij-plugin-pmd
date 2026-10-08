package software.xdev.pmd.model.config.thirdpartycplocation.file;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import com.intellij.openapi.project.Project;

import software.xdev.pmd.config.state.project.thirdpartycp.FileThirdPartyCPLocationState;
import software.xdev.pmd.model.config.thirdpartycplocation.ThirdPartyCPLocationFactory;
import software.xdev.pmd.util.io.ProjectFilePaths;


public abstract class FileThirdPartyCPLocationFactory<L extends FileThirdPartyCPLocation>
	extends ThirdPartyCPLocationFactory<L, FileThirdPartyCPLocationState, Path>
{
	protected final ProjectFilePaths projectFilePaths;
	
	protected FileThirdPartyCPLocationFactory(final Project project)
	{
		this.projectFilePaths = project.getService(ProjectFilePaths.class);
	}
	
	protected Path checkFileExists(final String absolutePath)
	{
		Objects.requireNonNull(absolutePath);
		if(absolutePath.isBlank())
		{
			throw new IllegalArgumentException("Empty path");
		}
		
		final Path path = Paths.get(absolutePath);
		if(!Files.exists(path))
		{
			throw new IllegalArgumentException("File does not exist at " + path);
		}
		return path;
	}
}
