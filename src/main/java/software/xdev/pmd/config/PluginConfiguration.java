package software.xdev.pmd.config;

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import net.jcip.annotations.Immutable;
import software.xdev.pmd.model.config.ConfigurationLocation;
import software.xdev.pmd.model.scope.ScanScope;


/**
 * Represents the entire persistent plugin configuration on project level as an immutable object. This is intended to be
 * a simple DTO without any business logic.
 */
@Immutable
public class PluginConfiguration
{
	private final ScanScope scanScope;
	private final boolean scrollToSource;
	private final SortedSet<ConfigurationLocation> locations;
	private final SortedSet<String> activeLocationIds;
	
	PluginConfiguration(
		@NotNull final ScanScope scanScope,
		final boolean scrollToSource,
		@NotNull final SortedSet<ConfigurationLocation> locations,
		@NotNull final SortedSet<String> activeLocationIds)
	{
		this.scanScope = scanScope;
		this.scrollToSource = scrollToSource;
		this.locations = Collections.unmodifiableSortedSet(locations);
		this.activeLocationIds = activeLocationIds.stream()
			.filter(Objects::nonNull)
			.collect(Collectors.toCollection(TreeSet::new));
	}
	
	@NotNull
	public ScanScope getScanScope()
	{
		return this.scanScope;
	}
	
	public boolean isScrollToSource()
	{
		return this.scrollToSource;
	}
	
	@NotNull
	public SortedSet<ConfigurationLocation> getLocations()
	{
		return this.locations;
	}
	
	@NotNull
	public Optional<ConfigurationLocation> getLocationById(@NotNull final String locationId)
	{
		return this.locations.stream()
			.filter(candidate -> candidate.getId().equals(locationId))
			.findFirst();
	}
	
	public SortedSet<String> getActiveLocationIds()
	{
		return this.activeLocationIds;
	}
	
	@NotNull
	public SortedSet<ConfigurationLocation> getActiveLocations()
	{
		return this.getActiveLocationIds().stream()
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
	
	@Override
	public boolean equals(final Object other)
	{
		if(this == other)
		{
			return true;
		}
		if(other == null || this.getClass() != other.getClass())
		{
			return false;
		}
		final PluginConfiguration otherDto = (PluginConfiguration)other;
		return Objects.equals(this.scanScope, otherDto.scanScope)
			&& Objects.equals(this.scrollToSource, otherDto.scrollToSource)
			&& Objects.equals(this.locations, otherDto.locations)
			&& Objects.equals(this.activeLocationIds, otherDto.activeLocationIds);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(
			this.scanScope,
			this.scrollToSource,
			this.locations,
			this.activeLocationIds);
	}
}
