package software.xdev.pmd.config;

import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.project.Project;

import software.xdev.pmd.model.config.ConfigurationLocation;
import software.xdev.pmd.model.config.ConfigurationLocationFactory;
import software.xdev.pmd.model.config.bundled.BundledConfig;
import software.xdev.pmd.model.scope.ScanScope;


public final class PluginConfigurationBuilder
{
	private boolean useSingleThread;
	private boolean showSuppressedWarnings;
	private boolean useCacheFile;
	private ScanScope scanScope;
	private SortedSet<ConfigurationLocation> locations;
	private SortedSet<String> activeLocationIds;
	
	public PluginConfigurationBuilder(final Project project)
	{
		this.showSuppressedWarnings = true;
		this.useCacheFile = true;
		this.scanScope = ScanScope.getDefaultValue();
		this.locations = BundledConfig.getAllBundledConfigs()
			.stream()
			.map(bc -> configurationLocationFactory(project).create(bc, project))
			.collect(Collectors.toCollection(TreeSet::new));
		this.activeLocationIds = Collections.emptySortedSet();
	}
	
	public PluginConfigurationBuilder(final PluginConfiguration copyFrom)
	{
		this.useSingleThread = copyFrom.useSingleThread();
		this.showSuppressedWarnings = copyFrom.showSuppressedWarnings();
		this.useCacheFile = copyFrom.useCacheFile();
		this.scanScope = copyFrom.scanScope();
		this.locations = copyFrom.locations();
		this.activeLocationIds = copyFrom.activeLocationIds();
	}
	
	public static PluginConfiguration copy(@NotNull final PluginConfiguration source)
	{
		return new PluginConfigurationBuilder(source).build();
	}
	
	public PluginConfigurationBuilder withUseSingleThread(@Nullable final Boolean useSingleThread)
	{
		if(useSingleThread != null)
		{
			this.useSingleThread = useSingleThread;
		}
		return this;
	}
	
	public PluginConfigurationBuilder withShowSuppressedWarnings(@Nullable final Boolean showSuppressedWarnings)
	{
		if(showSuppressedWarnings != null)
		{
			this.showSuppressedWarnings = showSuppressedWarnings;
		}
		return this;
	}
	
	public PluginConfigurationBuilder withUseCacheFile(@Nullable final Boolean useCacheFile)
	{
		if(useCacheFile != null)
		{
			this.useCacheFile = useCacheFile;
		}
		return this;
	}
	
	public PluginConfigurationBuilder withActiveLocationIds(@NotNull final SortedSet<String> newActiveLocationIds)
	{
		this.activeLocationIds = newActiveLocationIds;
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
			this.useSingleThread,
			this.showSuppressedWarnings,
			this.useCacheFile,
			this.scanScope,
			Collections.unmodifiableSortedSet(Objects.requireNonNullElseGet(this.locations, TreeSet::new)),
			this.activeLocationIds != null
				? this.activeLocationIds.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(TreeSet::new))
				: new TreeSet<>(),
			new PluginConfiguration.Cache());
	}
	
	private static ConfigurationLocationFactory configurationLocationFactory(final Project project)
	{
		return project.getService(ConfigurationLocationFactory.class);
	}
}
