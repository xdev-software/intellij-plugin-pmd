package software.xdev.pmd.config;

import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.project.Project;

import software.xdev.pmd.model.config.ConfigurationLocation;
import software.xdev.pmd.model.config.ConfigurationLocationFactory;
import software.xdev.pmd.model.config.bundled.BundledConfig;
import software.xdev.pmd.model.scope.ScanScope;


public final class PluginConfigurationBuilder
{
	private ScanScope scanScope;
	private boolean scrollToSource;
	private SortedSet<ConfigurationLocation> locations;
	private SortedSet<String> activeLocationIds;
	
	private PluginConfigurationBuilder(
		@NotNull final ScanScope scanScope,
		final boolean scrollToSource,
		@NotNull final SortedSet<ConfigurationLocation> locations,
		@NotNull final SortedSet<String> activeLocationIds)
	{
		this.scanScope = scanScope;
		this.scrollToSource = scrollToSource;
		this.locations = locations;
		this.activeLocationIds = activeLocationIds;
	}
	
	public static PluginConfigurationBuilder defaultConfiguration(@NotNull final Project project)
	{
		final SortedSet<ConfigurationLocation> defaultLocations = new TreeSet<>();
		
		BundledConfig.getAllBundledConfigs()
			.stream()
			.map(bc -> configurationLocationFactory(project).create(bc, project))
			.forEach(defaultLocations::add);
		
		return new PluginConfigurationBuilder(
			ScanScope.getDefaultValue(),
			false,
			defaultLocations,
			Collections.emptySortedSet());
	}
	
	public static PluginConfigurationBuilder from(@NotNull final PluginConfiguration source)
	{
		return new PluginConfigurationBuilder(
			source.getScanScope(),
			source.isScrollToSource(),
			source.getLocations(),
			source.getActiveLocationIds());
	}
	
	public PluginConfigurationBuilder withActiveLocationIds(@NotNull final SortedSet<String> newActiveLocationIds)
	{
		this.activeLocationIds = newActiveLocationIds;
		return this;
	}
	
	public PluginConfigurationBuilder withScrollToSource(final boolean newScrollToSource)
	{
		this.scrollToSource = newScrollToSource;
		return this;
	}
	
	public PluginConfigurationBuilder withLocations(@NotNull final SortedSet<ConfigurationLocation> newLocations)
	{
		this.locations = newLocations;
		return this;
	}
	
	public PluginConfigurationBuilder withScanScope(@NotNull final ScanScope newScanScope)
	{
		this.scanScope = newScanScope;
		return this;
	}
	
	public PluginConfiguration build()
	{
		return new PluginConfiguration(
			this.scanScope,
			this.scrollToSource,
			Objects.requireNonNullElseGet(this.locations, TreeSet::new),
			Objects.requireNonNullElseGet(this.activeLocationIds, TreeSet::new));
	}
	
	private static ConfigurationLocationFactory configurationLocationFactory(final Project project)
	{
		return project.getService(ConfigurationLocationFactory.class);
	}
}
