package software.xdev.pmd.model.config.file;

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
	protected long nextReloadRuleSetMs;
	protected Instant lastModifiedFileTime;
	protected String location;
	protected Path locationPath;
	protected String description;
	
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
	public String getLocation()
	{
		return this.location;
	}
	
	@Override
	public void setLocation(final String location)
	{
		this.location = location;
		this.locationPath = this.getLocationPath();
	}
	
	protected String getRealLocation()
	{
		return this.projectFilePaths().toSystemPath(this.getLocation());
	}
	
	protected Path getLocationPath()
	{
		return Paths.get(this.getRealLocation());
	}
	
	@Override
	public void setDescription(final String description)
	{
		this.description = description;
	}
	
	@Override
	public String getDescription()
	{
		return this.description;
	}
	
	@Nullable
	protected Instant lastModifiedTimeFromLocation()
	{
		try
		{
			return Files.getLastModifiedTime(this.locationPath).toInstant();
		}
		catch(final IOException e)
		{
			return null;
		}
	}
	
	@SuppressWarnings("checkstyle:IllegalIdentifierName")
	@Override
	protected synchronized RuleSet loadRuleSet() throws IOException
	{
		this.nextReloadRuleSetMs = System.currentTimeMillis() + 10 * 1000;
		
		final RuleSet ruleSet = new RuleSetLoader().loadFromString(
			this.getLocation(),
			new String(Files.readAllBytes(this.locationPath)));
		this.lastModifiedFileTime = this.lastModifiedTimeFromLocation();
		return ruleSet;
	}
	
	@Override
	protected boolean shouldReloadRuleSet()
	{
		// Check if recently checked
		return System.currentTimeMillis() > this.nextReloadRuleSetMs
			// Check if file was modified
			&& (this.lastModifiedFileTime == null
			|| !this.lastModifiedFileTime.equals(this.lastModifiedTimeFromLocation()));
	}
	
	@NotNull
	protected ProjectFilePaths projectFilePaths()
	{
		return this.getProject().getService(ProjectFilePaths.class);
	}
}
