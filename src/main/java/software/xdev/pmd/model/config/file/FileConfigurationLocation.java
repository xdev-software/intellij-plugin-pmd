package software.xdev.pmd.model.config.file;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.project.Project;

import net.sourceforge.pmd.lang.rule.RuleSet;
import software.xdev.pmd.model.config.ConfigurationLocation;
import software.xdev.pmd.model.config.ConfigurationType;
import software.xdev.pmd.model.config.file.pmd.DefaultRuleSetLoaderCreator;
import software.xdev.pmd.model.config.file.pmd.LoadFromStringRuleSetLoaderWorkaround;
import software.xdev.pmd.util.io.ProjectFilePaths;


/**
 * A configuration file on a mounted file system.
 */
public class FileConfigurationLocation extends ConfigurationLocation
{
	protected long nextReloadRuleSetMs;
	// Use WeakReference to prevent memory leak
	protected WeakReference<ClassLoader> previouslyUsedClassLoaderRef;
	
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
	protected synchronized RuleSet loadRuleSet(final ClassLoader classLoader) throws IOException
	{
		this.nextReloadRuleSetMs = System.currentTimeMillis() + 10 * 1000;
		
		// Do this here due to IOEx
		final String rulesetXmlContent = new String(Files.readAllBytes(this.locationPath));
		final RuleSet ruleSet = DefaultRuleSetLoaderCreator.createAndLoad(rsl ->
			LoadFromStringRuleSetLoaderWorkaround.loadFromString(
				rsl.loadResourcesWith(classLoader),
				this.getLocation(),
				rulesetXmlContent));
		this.lastModifiedFileTime = this.lastModifiedTimeFromLocation();
		this.previouslyUsedClassLoaderRef = new WeakReference<>(classLoader);
		return ruleSet;
	}
	
	@Override
	protected boolean shouldReloadRuleSet(final ClassLoader classLoader)
	{
		// Check if classloader mismatch
		return this.previouslyUsedClassLoaderRef == null || this.previouslyUsedClassLoaderRef.get() != classLoader
			// Check if recently checked
			|| System.currentTimeMillis() > this.nextReloadRuleSetMs
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
