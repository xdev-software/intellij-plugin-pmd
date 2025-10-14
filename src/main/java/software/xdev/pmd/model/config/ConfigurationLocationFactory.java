package software.xdev.pmd.model.config;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.scope.packageSet.NamedScope;

import software.xdev.pmd.model.config.bundled.BundledConfig;
import software.xdev.pmd.model.config.bundled.BundledConfigurationLocation;
import software.xdev.pmd.model.config.file.FileConfigurationLocation;
import software.xdev.pmd.model.config.file.RelativeFileConfigurationLocation;


/**
 * Factory for configuration location objects.
 */
public class ConfigurationLocationFactory
{
	/**
	 * We maintain a map of all current locations, to avoid recreating identical objects. This allows us to ensure that
	 * updates to one location (e.g. a URL change) are visible to other modules with a reference to the given location.
	 */
	private final Map<ConfigurationLocation, ConfigurationLocation> instanceCache =
		Collections.synchronizedMap(new WeakHashMap<>());
	
	/**
	 * Create a new location.
	 *
	 * @param project     the project this location is associated with.
	 * @param type        the type.
	 * @param location    the location.
	 * @param description the optional description.
	 * @param namedScope  the {@link NamedScope} for this ConfigurationLocation
	 * @return the location.
	 */
	public @NotNull ConfigurationLocation create(
		final Project project,
		final String id,
		final ConfigurationType type,
		final String location,
		final String description,
		final NamedScope namedScope)
	{
		if(type == null)
		{
			throw new IllegalArgumentException("Type is required");
		}
		
		final ConfigurationLocation configurationLocation = switch(type)
		{
			case LOCAL_FILE -> new FileConfigurationLocation(project, id);
			case PROJECT_RELATIVE -> new RelativeFileConfigurationLocation(project, id);
			case BUNDLED -> new BundledConfigurationLocation(BundledConfig.fromId(id), project);
		};
		
		configurationLocation.setLocation(location);
		configurationLocation.setDescription(description);
		configurationLocation.setNamedScope(namedScope);
		
		final ConfigurationLocation cachedLocation = this.instanceCache.get(configurationLocation);
		if(cachedLocation != null)
		{
			return cachedLocation;
		}
		
		this.instanceCache.put(configurationLocation, configurationLocation);
		return configurationLocation;
	}
	
	public @NotNull BundledConfigurationLocation create(
		@NotNull final BundledConfig bundledConfig,
		@NotNull final Project project)
	{
		return new BundledConfigurationLocation(bundledConfig, project);
	}
}
