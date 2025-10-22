package software.xdev.pmd.model.config.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.project.Project;

import net.sourceforge.pmd.lang.rule.RuleSet;
import net.sourceforge.pmd.lang.rule.RuleSetLoader;
import software.xdev.pmd.model.config.ConfigurationLocation;
import software.xdev.pmd.model.config.ConfigurationType;
import software.xdev.pmd.util.io.ProjectFilePaths;


/**
 * A configuration file on a mounted file system.
 */
public class FileConfigurationLocation extends ConfigurationLocation
{
	private long lastLoadedRuleSetMs;
	private Instant lastModifiedFileTime;
	
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
	
	protected Path getLocationPath()
	{
		return Paths.get(this.getLocation());
	}
	
	@Nullable
	protected Instant lastModifiedTimeFromLocation()
	{
		try
		{
			return Files.getLastModifiedTime(this.getLocationPath()).toInstant();
		}
		catch(final IOException e)
		{
			return null;
		}
	}
	
	@Override
	protected synchronized RuleSet loadRuleSet() throws IOException
	{
		final RuleSet ruleSet = new RuleSetLoader().loadFromString(
			this.getLocation(),
			new String(Files.readAllBytes(this.getLocationPath())));
		this.lastLoadedRuleSetMs = System.currentTimeMillis();
		this.lastModifiedFileTime = this.lastModifiedTimeFromLocation();
		return ruleSet;
	}
	
	@Override
	protected boolean shouldReloadRuleSet()
	{
		// Check if recently checked
		return System.currentTimeMillis() - this.lastLoadedRuleSetMs >= 10 * 1000
			// Check if file was modified
			&& (this.lastModifiedFileTime == null
			|| !this.lastModifiedFileTime.equals(this.lastModifiedTimeFromLocation()));
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
