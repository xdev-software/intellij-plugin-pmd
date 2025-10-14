package software.xdev.pmd.model.config.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.project.Project;

import software.xdev.pmd.model.config.ConfigurationLocation;
import software.xdev.pmd.model.config.ConfigurationType;
import software.xdev.pmd.model.config.PMDRuleSetValidator;
import software.xdev.pmd.util.io.ProjectFilePaths;


/**
 * A configuration file on a mounted file system.
 */
public class FileConfigurationLocation extends ConfigurationLocation
{
	/**
	 * Create a new file configuration.
	 *
	 * @param project the project.
	 */
	public FileConfigurationLocation(
		@NotNull final Project project,
		@NotNull final String id)
	{
		this(project, id, ConfigurationType.LOCAL_FILE);
	}
	
	public FileConfigurationLocation(
		@NotNull final Project project,
		@NotNull final String id,
		@NotNull final ConfigurationType configurationType)
	{
		super(id, configurationType, project);
	}
	
	@Override
	public File getBaseDir()
	{
		final String location = this.getLocation();
		if(location != null)
		{
			final File locationFile = new File(location);
			if(locationFile.exists())
			{
				return locationFile.getParentFile();
			}
		}
		
		return null;
	}
	
	@Override
	public String getLocation()
	{
		return this.projectFilePaths().detokenize(super.getLocation());
	}
	
	@Override
	public void setLocation(final String location)
	{
		if(location == null || location.isBlank())
		{
			throw new IllegalArgumentException("A non-blank location is required");
		}
		
		super.setLocation(this.projectFilePaths().tokenise(location));
	}
	
	@Override
	public void validate() throws IOException
	{
		PMDRuleSetValidator.validateOrThrow(new String(Files.readAllBytes(Paths.get(this.getLocation()))));
	}
	
	@NotNull
	protected ProjectFilePaths projectFilePaths()
	{
		return this.getProject().getService(ProjectFilePaths.class);
	}
	
	@Override
	public Object clone()
	{
		return this.cloneCommonPropertiesTo(new FileConfigurationLocation(this.getProject(), this.getId()));
	}
}
