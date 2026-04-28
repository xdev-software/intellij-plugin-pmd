package software.xdev.pmd.config;

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
public record PluginConfiguration(
	boolean useSingleThread,
	boolean showSuppressedWarnings,
	boolean useCacheFile,
	ScanScope scanScope,
	SortedSet<ConfigurationLocation> locations,
	SortedSet<String> activeLocationIds,
	Cache cache
)
{
	@SuppressWarnings("checkstyle:VisibilityModifier")
	static class Cache
	{
		Map<String, ConfigurationLocation> idLocationCache;
		SortedSet<ConfigurationLocation> activeLocationCache;
	}
	
	@Nullable
	public ConfigurationLocation getLocationById(@NotNull final String locationId)
	{
		if(this.cache.idLocationCache == null)
		{
			this.cache.idLocationCache = this.locations.stream()
				.collect(Collectors.toMap(ConfigurationLocation::getId, Function.identity()));
		}
		
		return this.cache.idLocationCache.get(locationId);
	}
	
	@NotNull
	public SortedSet<ConfigurationLocation> getActiveLocations()
	{
		if(this.cache.activeLocationCache == null)
		{
			this.cache.activeLocationCache = this.activeLocationIds().stream()
				.map(this::getLocationById)
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(TreeSet::new));
		}
		return this.cache.activeLocationCache;
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
}
