package software.xdev.pmd.config.state.project;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import com.intellij.util.xmlb.annotations.XCollection;

import software.xdev.pmd.config.state.project.thirdpartycp.FileThirdPartyCPLocationState;
import software.xdev.pmd.config.state.project.thirdpartycp.MavenThirdPartyCPLocationState;
import software.xdev.pmd.config.state.project.thirdpartycp.ThirdPartyCPLocationState;
import software.xdev.pmd.model.config.thirdpartycplocation.ThirdPartyCPLocation;
import software.xdev.pmd.model.config.thirdpartycplocation.ThirdPartyCPLocationFactory;
import software.xdev.pmd.model.config.thirdpartycplocation.file.absolute.AbsoluteFileThirdPartyCPLocation;
import software.xdev.pmd.model.config.thirdpartycplocation.file.absolute.AbsoluteFileThirdPartyCPLocationFactory;
import software.xdev.pmd.model.config.thirdpartycplocation.file.relative.RelativeFileThirdPartyCPLocation;
import software.xdev.pmd.model.config.thirdpartycplocation.file.relative.RelativeFileThirdPartyCPLocationFactory;
import software.xdev.pmd.model.config.thirdpartycplocation.maven.MavenThirdPartyCPLocation;
import software.xdev.pmd.model.config.thirdpartycplocation.maven.MavenThirdPartyCPLocationFactory;


// NOTE: Must be located in same package or plugin state import will fail during boot!
public class ThirdPartyCPState
{
	private static final Logger LOG = Logger.getInstance(ThirdPartyCPState.class);
	
	@XCollection
	List<String> activeIds;
	
	@MapAnnotation
	List<MavenThirdPartyCPLocationState> maven;
	
	@MapAnnotation
	List<FileThirdPartyCPLocationState> absoluteFile;
	
	@MapAnnotation
	List<FileThirdPartyCPLocationState> relativeFile;
	
	@SuppressWarnings("unused")
	public ThirdPartyCPState()
	{
		// for serialization
	}
	
	public ThirdPartyCPState(
		final List<String> activeIds,
		final List<MavenThirdPartyCPLocationState> maven,
		final List<FileThirdPartyCPLocationState> absoluteFile,
		final List<FileThirdPartyCPLocationState> relativeFile)
	{
		this.activeIds = activeIds;
		this.maven = maven;
		this.absoluteFile = absoluteFile;
		this.relativeFile = relativeFile;
	}
	
	public static ThirdPartyCPState create(
		final List<ThirdPartyCPLocation> thirdPartyCPLocations)
	{
		if(thirdPartyCPLocations.isEmpty())
		{
			return null;
		}
		
		final Map<Class<? extends ThirdPartyCPLocation>, List<ThirdPartyCPLocation>> groupedByClazz =
			thirdPartyCPLocations.stream().collect(Collectors.groupingBy(ThirdPartyCPLocation::getClass));
		
		return new ThirdPartyCPState(
			thirdPartyCPLocations.stream()
				.map(ThirdPartyCPLocation::id)
				.distinct()
				.toList(),
			create(
				groupedByClazz,
				MavenThirdPartyCPLocation.class,
				loc -> new MavenThirdPartyCPLocationState(
					loc.id(),
					loc.mavenId().groupId(),
					loc.mavenId().artifactId(),
					loc.mavenId().version())),
			create(
				groupedByClazz,
				AbsoluteFileThirdPartyCPLocation.class,
				loc -> new FileThirdPartyCPLocationState(loc.id(), loc.location())),
			create(
				groupedByClazz,
				RelativeFileThirdPartyCPLocation.class,
				loc -> new FileThirdPartyCPLocationState(loc.id(), loc.location()))
		);
	}
	
	@Nullable
	private static <
		L extends ThirdPartyCPLocation,
		S extends ThirdPartyCPLocationState>
	List<S> create(
		final Map<Class<? extends ThirdPartyCPLocation>, List<ThirdPartyCPLocation>> groupedByClazz,
		final Class<L> clazz,
		final Function<L, S> toState)
	{
		final List<ThirdPartyCPLocation> thirdPartyCPLocations = groupedByClazz.get(clazz);
		if(thirdPartyCPLocations == null || thirdPartyCPLocations.isEmpty())
		{
			return null;
		}
		return thirdPartyCPLocations
			.stream()
			.map(clazz::cast)
			.map(toState)
			.filter(Objects::nonNull)
			.toList();
	}
	
	public List<ThirdPartyCPLocation> populate(@NotNull final Project project)
	{
		if(this.activeIds == null || this.activeIds.isEmpty())
		{
			return List.of();
		}
		
		final Set<String> activeIdsFastAccess = Set.copyOf(this.activeIds);
		
		final Map<String, ThirdPartyCPLocation> availableLocations = Stream.of(
				new LocationPopulator<>(MavenThirdPartyCPLocationFactory.class, this.maven),
				new LocationPopulator<>(AbsoluteFileThirdPartyCPLocationFactory.class, this.absoluteFile),
				new LocationPopulator<>(RelativeFileThirdPartyCPLocationFactory.class, this.relativeFile))
			.map(p ->
				p.populate(activeIdsFastAccess, project))
			.flatMap(List::stream)
			.collect(Collectors.toMap(ThirdPartyCPLocation::id, Function.identity()));
		
		return this.activeIds.stream()
			.map(availableLocations::get)
			.toList();
	}
	
	public record LocationPopulator<
		L extends ThirdPartyCPLocation,
		S extends ThirdPartyCPLocationState,
		F extends ThirdPartyCPLocationFactory<L, S, ?>>(
		Class<F> factoryClazz,
		List<S> persistedStates)
	{
		public List<ThirdPartyCPLocation> populate(
			final Set<String> activeIds,
			final Project project)
		{
			if(this.persistedStates == null || this.persistedStates.isEmpty())
			{
				return List.of();
			}
			final F factory = project.getService(this.factoryClazz);
			return this.persistedStates.stream()
				.filter(s -> activeIds.contains(s.id()))
				.map(s -> {
					try
					{
						return (ThirdPartyCPLocation)factory.fromPersisted(s);
					}
					catch(final Exception ex)
					{
						LOG.error("Encountered problem while populating location[id=" + s.id() + "]", ex);
						return null;
					}
				})
				.filter(Objects::nonNull)
				.toList();
		}
	}
}
