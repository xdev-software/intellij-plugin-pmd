package software.xdev.pmd.model.config.file;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.project.Project;

import software.xdev.pmd.model.config.ConfigurationType;


/**
 * A configuration file on a mounted file system which will always be referred to by a path relative to the project
 * path.
 */
public class RelativeFileConfigurationLocation extends FileConfigurationLocation
{
	private static final String LEGACY_IDEA_PROJECT_DIR_START = "$PROJECT_DIR$/";
	
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
		super.setLocation(location.startsWith(LEGACY_IDEA_PROJECT_DIR_START)
			? location.substring(LEGACY_IDEA_PROJECT_DIR_START.length())
			: location);
	}
	
	@Override
	protected String getRealLocation()
	{
		return this.projectFilePaths().makeProjectRelativePathAbsolute(super.getRealLocation());
	}
}
