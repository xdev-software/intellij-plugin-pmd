package software.xdev.pmd.config;

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import software.xdev.pmd.model.config.ConfigurationLocation;
import software.xdev.pmd.model.scope.ScanScope;


/**
 * Represents the entire persistent plugin configuration on project level as an immutable object. This is intended to be
 * a simple DTO without any business logic.
 */
public record PluginConfiguration(
	ScanScope scanScope,
	SortedSet<ConfigurationLocation> locations,
	SortedSet<String> activeLocationIds
)
{
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
	
	@NotNull
	public Optional<ConfigurationLocation> getLocationById(@NotNull final String locationId)
	{
		return this.locations.stream()
			.filter(candidate -> candidate.getId().equals(locationId))
			.findFirst();
	}
	
	@NotNull
	public SortedSet<ConfigurationLocation> getActiveLocations()
	{
		return this.activeLocationIds().stream()
			.map(idToFind -> this.locations.stream()
				.filter(candidate -> candidate.getId().equals(idToFind))
				.findFirst())
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toCollection(TreeSet::new));
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
