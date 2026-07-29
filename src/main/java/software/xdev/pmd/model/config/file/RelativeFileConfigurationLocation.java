package software.xdev.pmd.model.config.file;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.project.Project;

import software.xdev.pmd.model.config.ConfigurationType;


/**
 * A configuration file on a mounted file system which will always be referred to by a path relative to the project
 * path.
 */
public class RelativeFileConfigurationLocation extends FileConfigurationLocation
{
	private static final String LEGACY_IDEA_PROJECT_DIR = "$PROJECT_DIR$";
	
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
	
	@SuppressWarnings("checkstyle:FinalParameters")
	@Override
	public void setLocation(String location)
	{
		// Detect legacy $PROJECT_DIR$ that was resolved during importing
		if(location.length() > 5
			// linux e.g. /abc/...
			&& (location.startsWith("/")
			// windows e.g. c:/abc/...
			|| location.charAt(1) == ':' && location.charAt(2) == '/'))
		{
			final String resolvedProjectDir = PathMacroManager.getInstance(this.getProject())
				.expandPath(LEGACY_IDEA_PROJECT_DIR);
			if(location.startsWith(resolvedProjectDir) && location.length() > resolvedProjectDir.length() + 1)
			{
				// Also cut away path separator
				location = location.substring(resolvedProjectDir.length() + 1);
			}
		}
		
		super.setLocation(location);
	}
	
	@Override
	protected String getRealLocation()
	{
		return this.projectFilePaths().makeProjectRelativePathAbsolute(super.getRealLocation());
	}
}
