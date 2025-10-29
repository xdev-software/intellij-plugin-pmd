package software.xdev.pmd.config;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import software.xdev.pmd.model.config.ConfigurationLocation;
import software.xdev.pmd.model.scope.ScanScope;


/**
 * Represents the entire persistent plugin configuration on project level as an immutable object. This is intended to be
 * a simple DTO without any business logic.
 */
public class PluginConfiguration
{
	private final ScanScope scanScope;
	private final SortedSet<ConfigurationLocation> locations;
	private final SortedSet<String> activeLocationIds;
	
	private Map<String, ConfigurationLocation> idLocationCache;
	private SortedSet<ConfigurationLocation> activeLocationCache;
	
	public PluginConfiguration(
		final ScanScope scanScope,
		final SortedSet<ConfigurationLocation> locations,
		final SortedSet<String> activeLocationIds)
	{
		this.scanScope = scanScope;
		this.locations = Collections.unmodifiableSortedSet(locations);
		this.activeLocationIds = activeLocationIds.stream()
			.filter(Objects::nonNull)
			.collect(Collectors.toCollection(TreeSet::new));
	}
	
	@Nullable
	public ConfigurationLocation getLocationById(@NotNull final String locationId)
	{
		if(this.idLocationCache == null)
		{
			this.idLocationCache = this.locations.stream()
				.collect(Collectors.toMap(ConfigurationLocation::getId, Function.identity()));
		}
		
		return this.idLocationCache.get(locationId);
	}
	
	@NotNull
	public SortedSet<ConfigurationLocation> getActiveLocations()
	{
		if(this.activeLocationCache == null)
		{
			this.activeLocationCache = this.activeLocationIds().stream()
				.map(this::getLocationById)
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(TreeSet::new));
		}
		return this.activeLocationCache;
	}
	
	public boolean hasChangedFrom(final Object other)
	{
		return this.equals(other) && this.locationsAreEqual((PluginConfiguration)other);
	}
	
	private boolean locationsAreEqual(final PluginConfiguration other)
	{
		final Iterator<ConfigurationLocation> locationIterator = this.locations.iterator();
		final Iterator<ConfigurationLocation> otherLocationIterator = other.locations.iterator();
		
		while(locationIterator.hasNext() && otherLocationIterator.hasNext())
		{
			if(locationIterator.next().hasChangedFrom(otherLocationIterator.next()))
			{
				return false;
			}
		}
		
		return this.locations.size() == other.locations.size();
	}
	
	public ScanScope scanScope()
	{
		return this.scanScope;
	}
	
	public SortedSet<ConfigurationLocation> locations()
	{
		return this.locations;
	}
	
	public SortedSet<String> activeLocationIds()
	{
		return this.activeLocationIds;
	}
}
