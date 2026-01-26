package software.xdev.pmd.model.config;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.project.Project;

import software.xdev.pmd.external.org.apache.shiro.lang.util.SoftHashMap;
import software.xdev.pmd.model.config.bundled.BundledConfig;
import software.xdev.pmd.model.config.bundled.BundledConfigurationLocation;
import software.xdev.pmd.model.config.file.FileConfigurationLocation;
import software.xdev.pmd.model.config.file.RelativeFileConfigurationLocation;


public class ConfigurationLocationFactory
{
	private final Map<CreateCacheKey, ConfigurationLocation> createCache =
		Collections.synchronizedMap(new SoftHashMap<>());
	
	
	record CreateCacheKey(
		String id,
		ConfigurationType type,
		String location,
		String description)
	{
	}
	
	
	/**
	 * We maintain a map of all current locations, to avoid recreating identical objects. This allows us to ensure that
	 * updates to one location (e.g. a URL change) are visible to other modules with a reference to the given location.
	 */
	private final Map<ConfigurationLocation, ConfigurationLocation> instanceDeduplicationCache =
		Collections.synchronizedMap(new WeakHashMap<>());
	
	/**
	 * Create a new location.
	 *
	 * @param project     the project this location is associated with.
	 * @param type        the type.
	 * @param location    the location.
	 * @param description the optional description.
	 * @return the location.
	 */
	public @NotNull ConfigurationLocation create(
		final Project project,
		final String id,
		final ConfigurationType type,
		final String location,
		final String description)
	{
		if(type == null)
		{
			throw new IllegalArgumentException("Type is required");
		}
		
		return this.createCache.computeIfAbsent(
			new CreateCacheKey(id, type, location, description),
			ignored -> {
				final ConfigurationLocation configurationLocation = switch(type)
				{
					case LOCAL_FILE -> new FileConfigurationLocation(project, id);
					case PROJECT_RELATIVE -> new RelativeFileConfigurationLocation(project, id);
					case BUNDLED -> new BundledConfigurationLocation(BundledConfig.fromId(id), project);
				};
				
				configurationLocation.setLocation(location);
				configurationLocation.setDescription(description);
				
				final ConfigurationLocation cachedLocation =
					this.instanceDeduplicationCache.get(configurationLocation);
				if(cachedLocation != null)
				{
					return cachedLocation;
				}
				
				this.instanceDeduplicationCache.put(configurationLocation, configurationLocation);
				return configurationLocation;
			});
	}
	
	public @NotNull BundledConfigurationLocation create(
		@NotNull final BundledConfig bundledConfig,
		@NotNull final Project project)
	{
		return new BundledConfigurationLocation(bundledConfig, project);
	}
}
