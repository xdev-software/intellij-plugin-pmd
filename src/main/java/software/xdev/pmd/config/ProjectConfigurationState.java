package software.xdev.pmd.config;

import static java.util.Objects.requireNonNullElseGet;
import static software.xdev.pmd.config.PluginConfigurationBuilder.defaultConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.ControlFlowException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Text;
import com.intellij.util.xmlb.annotations.XCollection;

import software.xdev.pmd.model.config.ConfigurationLocationFactory;
import software.xdev.pmd.model.config.ConfigurationType;
import software.xdev.pmd.model.config.bundled.BundledConfig;
import software.xdev.pmd.model.scope.NamedScopeHelper;
import software.xdev.pmd.model.scope.ScanScope;


@State(name = "PMD-X", storages = {@Storage("pmd-x.xml")})
public class ProjectConfigurationState implements PersistentStateComponent<ProjectConfigurationState.ProjectSettings>
{
	private static final Logger LOG = Logger.getInstance(ProjectConfigurationState.class);
	
	private final Project project;
	
	private ProjectSettings projectSettings;
	
	public ProjectConfigurationState(@NotNull final Project project)
	{
		this.project = project;
		
		this.projectSettings = this.defaultProjectSettings();
	}
	
	private ProjectSettings defaultProjectSettings()
	{
		return ProjectSettings.create(defaultConfiguration(this.project).build());
	}
	
	@Override
	public ProjectSettings getState()
	{
		return this.projectSettings;
	}
	
	@Override
	public void loadState(@NotNull final ProjectSettings sourceProjectSettings)
	{
		this.projectSettings = sourceProjectSettings;
	}
	
	@NotNull
	PluginConfigurationBuilder populate(@NotNull final PluginConfigurationBuilder builder)
	{
		return this.projectSettings.populate(builder, this.project);
	}
	
	void setCurrentConfig(@NotNull final PluginConfiguration currentPluginConfig)
	{
		this.projectSettings = ProjectSettings.create(currentPluginConfig);
	}
	
	static class ProjectSettings
	{
		@Attribute
		private String serialisationVersion;
		
		// TODO
		// Thread Count (null = auto)
		// Batch process modules?
		@Tag
		private String scanScope;
		@Tag
		private boolean scrollToSource;
		@XCollection
		private List<String> activeLocationIds;
		@MapAnnotation
		private List<ConfigurationLocation> locations;
		
		static ProjectSettings create(@NotNull final PluginConfiguration currentPluginConfig)
		{
			final ProjectSettings projectSettings = new ProjectSettings();
			
			projectSettings.serialisationVersion = "1";
			
			projectSettings.scanScope = currentPluginConfig.getScanScope().name();
			projectSettings.scrollToSource = currentPluginConfig.isScrollToSource();
			
			projectSettings.activeLocationIds = new ArrayList<>(currentPluginConfig.getActiveLocationIds());
			
			projectSettings.locations = currentPluginConfig.getLocations().stream()
				.map(location -> new ConfigurationLocation(
					location.getId(),
					location.getType().name(),
					location.getRawLocation(),
					location.getDescription(),
					location.getNamedScope().map(NamedScope::getScopeId).orElse("")
				))
				.collect(Collectors.toList());
			
			return projectSettings;
		}
		
		@SuppressWarnings("unused")
			// for serialisation
		ProjectSettings()
		{
		}
		
		PluginConfigurationBuilder populate(
			@NotNull final PluginConfigurationBuilder builder,
			@NotNull final Project project)
		{
			return builder
				.withScanScope(this.lookupScanScope())
				.withScrollToSource(this.scrollToSource)
				.withLocations(this.deserializeLocations(project))
				.withActiveLocationIds(new TreeSet<>(requireNonNullElseGet(
					this.activeLocationIds,
					ArrayList::new)));
		}
		
		@NotNull
		private TreeSet<software.xdev.pmd.model.config.ConfigurationLocation> deserializeLocations(
			@NotNull final Project project)
		{
			final TreeSet<software.xdev.pmd.model.config.ConfigurationLocation> configurationLocations
				= requireNonNullElseGet(this.locations, () -> new ArrayList<ConfigurationLocation>()).stream()
				.map(location -> this.deserializeLocation(project, location))
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(TreeSet::new));
			BundledConfig.getAllBundledConfigs().forEach(bundleConfig -> {
				if(configurationLocations.stream().noneMatch(bundleConfig::matches))
				{
					configurationLocations.add(this.configurationLocationFactory(project)
						.create(bundleConfig, project));
				}
			});
			return configurationLocations;
		}
		
		@NotNull
		private ScanScope lookupScanScope()
		{
			if(this.scanScope != null)
			{
				try
				{
					return ScanScope.valueOf(this.scanScope);
				}
				catch(final IllegalArgumentException e)
				{
					// settings got messed up (manual edit?) - use default
				}
			}
			return ScanScope.getDefaultValue();
		}
		
		private ConfigurationLocationFactory configurationLocationFactory(@NotNull final Project project)
		{
			return project.getService(ConfigurationLocationFactory.class);
		}
		
		@Nullable
		private software.xdev.pmd.model.config.ConfigurationLocation deserializeLocation(
			@NotNull final Project project,
			@Nullable final ConfigurationLocation location)
		{
			if(location == null)
			{
				return null;
			}
			
			try
			{
				return this.configurationLocationFactory(project).create(
					project,
					location.id,
					ConfigurationType.parse(location.type),
					Objects.requireNonNullElse(location.location, "").trim(),
					location.description,
					NamedScopeHelper.getScopeByIdWithDefaultFallback(project, location.scope));
			}
			catch(final Exception e)
			{
				if(e instanceof ControlFlowException)
				{
					throw e;
				}
				LOG.error("Failed to deserialize " + location, e);
				return null;
			}
		}
	}
	
	
	static class ConfigurationLocation
	{
		@Attribute
		private String id;
		@Attribute
		private String type;
		@Attribute
		private String scope;
		@Attribute
		private String description;
		@Text
		private String location;
		
		@SuppressWarnings("unused")
			// serialisation
		ConfigurationLocation()
		{
		}
		
		ConfigurationLocation(
			final String id,
			final String type,
			final String location,
			final String description,
			final String scope)
		{
			this.id = id;
			this.type = type;
			this.scope = scope;
			this.description = description;
			this.location = location;
		}
		
		@Override
		public String toString()
		{
			return "ConfigurationLocation{"
				+ "id='" + this.id + '\''
				+ ", type='" + this.type + '\''
				+ ", scope='" + this.scope + '\''
				+ ", description='" + this.description + '\''
				+ ", location='" + this.location + '\''
				+ '}';
		}
	}
}
