package software.xdev.pmd.config.state.project;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
	LinkedHashSet<String> activeIds;
	
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
	
	public static ThirdPartyCPState create(
		final List<ThirdPartyCPLocation> thirdPartyCPLocations)
	{
		if(thirdPartyCPLocations.isEmpty())
		{
			return null;
		}
		
		final ThirdPartyCPState state = new ThirdPartyCPState();
		state.activeIds = thirdPartyCPLocations.stream()
			.map(ThirdPartyCPLocation::id)
			.collect(Collectors.toCollection(LinkedHashSet::new));
		
		final Map<Class<? extends ThirdPartyCPLocation>, List<ThirdPartyCPLocation>> groupedByClazz =
			thirdPartyCPLocations.stream().collect(Collectors.groupingBy(ThirdPartyCPLocation::getClass));
		
		state.maven = create(
			groupedByClazz,
			MavenThirdPartyCPLocation.class,
			loc -> new MavenThirdPartyCPLocationState(
				loc.id(),
				loc.mavenId().groupId(),
				loc.mavenId().artifactId(),
				loc.mavenId().version()));
		
		state.absoluteFile = create(
			groupedByClazz,
			AbsoluteFileThirdPartyCPLocation.class,
			loc -> new FileThirdPartyCPLocationState(loc.id(), loc.location()));
		
		state.relativeFile = create(
			groupedByClazz,
			RelativeFileThirdPartyCPLocation.class,
			loc -> new FileThirdPartyCPLocationState(loc.id(), loc.location()));
		
		return state;
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
		final Map<String, ThirdPartyCPLocation> availableLocations = Stream.of(
				this.populateLocations(MavenThirdPartyCPLocationFactory.class, project, this.maven),
				this.populateLocations(AbsoluteFileThirdPartyCPLocationFactory.class, project, this.absoluteFile),
				this.populateLocations(RelativeFileThirdPartyCPLocationFactory.class, project, this.relativeFile))
			.flatMap(List::stream)
			.collect(Collectors.toMap(ThirdPartyCPLocation::id, Function.identity()));
		
		return this.activeIds.stream()
			.map(availableLocations::get)
			.toList();
	}
	
	private <
		L extends ThirdPartyCPLocation,
		S extends ThirdPartyCPLocationState,
		F extends ThirdPartyCPLocationFactory<L, S, ?>>
	List<ThirdPartyCPLocation> populateLocations(
		final Class<F> factoryClazz,
		final Project project,
		final List<S> persistedStates
	)
	{
		if(persistedStates == null || persistedStates.isEmpty())
		{
			return List.of();
		}
		final F factory = project.getService(factoryClazz);
		return persistedStates.stream()
			.filter(s -> this.activeIds.contains(s.id()))
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
