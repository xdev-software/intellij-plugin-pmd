package software.xdev.pmd.model.config.file;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.project.Project;

import software.xdev.pmd.model.config.ConfigurationType;
import software.xdev.pmd.util.io.ProjectFilePaths;


/**
 * A configuration file on a mounted file system which will always be referred to by a path relative to the project
 * path.
 */
public class RelativeFileConfigurationLocation extends FileConfigurationLocation
{
	public RelativeFileConfigurationLocation(
		@NotNull final Project project,
		@NotNull final String id)
	{
		super(project, id, ConfigurationType.PROJECT_RELATIVE);
	}
	
	@Override
	public boolean canBeResolvedInDefaultProject()
	{
		return false;
	}
	
	@Override
	public void setLocation(final String location)
	{
		if(location == null || location.isBlank())
		{
			throw new IllegalArgumentException("A non-blank location is required");
		}
		
		final ProjectFilePaths projectFilePaths = this.projectFilePaths();
		super.setLocation(projectFilePaths.tokenise(
			projectFilePaths.makeProjectRelative(
				projectFilePaths.detokenize(location))));
	}
}
