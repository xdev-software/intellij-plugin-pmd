package software.xdev.pmd.config;

import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;

import software.xdev.pmd.model.config.ConfigurationLocation;


public class ConfigurationLocationSource
{
	private final Project project;
	
	public ConfigurationLocationSource(@NotNull final Project project)
	{
		this.project = project;
	}
	
	public SortedSet<ConfigurationLocation> getConfigurationLocations(@Nullable final Module module)
	{
		if(module != null)
		{
			final ModuleConfigurationState moduleConfiguration = this.moduleConfiguration(module);
			if(moduleConfiguration.isExcluded())
			{
				return Collections.emptySortedSet();
			}
			
			final PluginConfiguration configuration = this.configurationManager().getCurrent();
			final TreeSet<ConfigurationLocation> moduleActiveConfigurations =
				moduleConfiguration.getActiveLocationIds().stream()
					.map(id -> configuration.getLocationById(id).orElse(null))
					.filter(Objects::nonNull)
					.collect(Collectors.toCollection(TreeSet::new));
			if(!moduleActiveConfigurations.isEmpty())
			{
				return moduleActiveConfigurations;
			}
		}
		
		return this.configurationManager().getCurrent().getActiveLocations();
	}
	
	private PluginConfigurationManager configurationManager()
	{
		return this.project.getService(PluginConfigurationManager.class);
	}
	
	private ModuleConfigurationState moduleConfiguration(final Module module)
	{
		return module.getService(ModuleConfigurationState.class);
	}
}
