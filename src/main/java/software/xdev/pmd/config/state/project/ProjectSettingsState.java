package software.xdev.pmd.config.state.project;

import static java.util.Objects.requireNonNullElseGet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.diagnostic.ControlFlowException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.XCollection;

import software.xdev.pmd.config.PluginConfiguration;
import software.xdev.pmd.config.PluginConfigurationBuilder;
import software.xdev.pmd.model.config.ConfigurationLocation;
import software.xdev.pmd.model.config.ConfigurationLocationFactory;
import software.xdev.pmd.model.config.ConfigurationType;
import software.xdev.pmd.model.config.bundled.BundledConfig;
import software.xdev.pmd.model.scope.NamedScopeHelper;
import software.xdev.pmd.model.scope.ScanScope;


public class ProjectSettingsState
{
	private static final Logger LOG = Logger.getInstance(ProjectSettingsState.class);
	
	@Attribute
	String serialisationVersion;
	
	// TODO
	// Thread Count (null = auto)
	// Batch process modules?
	@Tag
	String scanScope;
	@Tag
	boolean scrollToSource;
	@XCollection
	List<String> activeLocationIds;
	@MapAnnotation
	List<ConfigurationLocationState> locations;
	
	static ProjectSettingsState create(@NotNull final PluginConfiguration currentPluginConfig)
	{
		final ProjectSettingsState projectSettings = new ProjectSettingsState();
		
		projectSettings.serialisationVersion = "1";
		
		projectSettings.scanScope = currentPluginConfig.scanScope().name();
		projectSettings.scrollToSource = currentPluginConfig.scrollToSource();
		
		projectSettings.activeLocationIds = new ArrayList<>(currentPluginConfig.activeLocationIds());
		
		projectSettings.locations = currentPluginConfig.locations().stream()
			.map(location -> new ConfigurationLocationState(
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
	public ProjectSettingsState()
	{
		// for serialisation
	}
	
	public PluginConfigurationBuilder populate(
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
	private TreeSet<ConfigurationLocation> deserializeLocations(
		@NotNull final Project project)
	{
		final ConfigurationLocationFactory configurationLocationFactory =
			project.getService(ConfigurationLocationFactory.class);
		
		final TreeSet<ConfigurationLocation> configurationLocations = this.locations != null
			? this.locations
			.stream()
			.map(location -> this.deserializeLocation(project, configurationLocationFactory, location))
			.filter(Objects::nonNull)
			.collect(Collectors.toCollection(TreeSet::new))
			: new TreeSet<>();
		
		BundledConfig.getAllBundledConfigs()
			.stream()
			.filter(bundledConfig -> configurationLocations.stream().noneMatch(bundledConfig::matches))
			.map(bundledConfig -> configurationLocationFactory.create(bundledConfig, project))
			.forEach(configurationLocations::add);
		
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
	
	@Nullable
	private ConfigurationLocation deserializeLocation(
		@NotNull final Project project,
		@NotNull final ConfigurationLocationFactory factory,
		@Nullable final ConfigurationLocationState location)
	{
		if(location == null)
		{
			return null;
		}
		
		try
		{
			return factory.create(
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
