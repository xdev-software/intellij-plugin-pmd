package software.xdev.pmd.config.plugin;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.google.common.base.Suppliers;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;

import software.xdev.pmd.util.io.ProjectFilePaths;


public record ThirdPartyClasspathConfigContainer(
	List<String> classpaths,
	List<String> absolutePathStrings,
	List<URL> urls
)
{
	public static ThirdPartyClasspathConfigContainer createFromUI(
		final Project project,
		final Collection<String> absolutePathStrings)
	{
		final Path projectPath = getProjectPath(project);
		final ProjectFilePaths projectFilePaths = project.getService(ProjectFilePaths.class);
		
		return createFromPersisted(
			() -> projectPath,
			projectFilePaths,
			absolutePathStrings.stream()
				.filter(Objects::nonNull)
				.filter(s -> !s.isBlank())
				.map(s -> {
					try
					{
						return Paths.get(s);
					}
					catch(final InvalidPathException ipe)
					{
						return null;
					}
				})
				.filter(Objects::nonNull)
				// Try to relativize
				.map(p -> {
					if(projectPath == null || !p.startsWith(projectPath))
					{
						return p;
					}
					
					try
					{
						return projectPath.relativize(p);
					}
					catch(final Exception ex)
					{
						return p;
					}
				})
				.map(Path::toString)
				.map(projectFilePaths::toUnixPath)
				.toList());
	}
	
	public static ThirdPartyClasspathConfigContainer createFromPersisted(
		final Project project,
		final Collection<String> classPaths)
	{
		return createFromPersisted(
			Suppliers.memoize(() -> getProjectPath(project)),
			project.getService(ProjectFilePaths.class),
			classPaths);
	}
	
	static ThirdPartyClasspathConfigContainer createFromPersisted(
		final Supplier<Path> projectPathSupplier,
		final ProjectFilePaths projectFilePaths,
		final Collection<String> classPaths)
	{
		if(classPaths == null || classPaths.isEmpty())
		{
			return createEmpty();
		}
		
		final List<String> validClassPaths = new ArrayList<>();
		final List<String> absolutePathStrings = new ArrayList<>(classPaths.size());
		final List<URL> urls = new ArrayList<>(classPaths.size());
		for(final String classpath : classPaths)
		{
			final Optional<Path> optAbsolutePath = Optional.ofNullable(classpath)
				.filter(s -> !s.isBlank())
				.map(projectFilePaths::toSystemPath)
				.map(Paths::get)
				.map(p -> {
					if(p.isAbsolute())
					{
						return p;
					}
					
					final Path projectPath = projectPathSupplier.get();
					return projectPath != null ? projectPath.resolve(p) : null;
				});
			optAbsolutePath
				.map(Path::toUri)
				.map(uri -> {
					try
					{
						return uri.toURL();
					}
					catch(final MalformedURLException ex)
					{
						return null;
					}
				})
				.ifPresent(url -> {
					validClassPaths.add(classpath);
					absolutePathStrings.add(optAbsolutePath.orElseThrow().toString());
					urls.add(url);
				});
		}
		
		return new ThirdPartyClasspathConfigContainer(
			Collections.unmodifiableList(validClassPaths),
			Collections.unmodifiableList(absolutePathStrings),
			Collections.unmodifiableList(urls)
		);
	}
	
	public static ThirdPartyClasspathConfigContainer createEmpty()
	{
		return new ThirdPartyClasspathConfigContainer(List.of(), List.of(), List.of());
	}
	
	private static @Nullable Path getProjectPath(final Project project)
	{
		final VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
		return projectDir != null ? projectDir.toNioPath() : null;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(!(o instanceof final ThirdPartyClasspathConfigContainer that))
		{
			return false;
		}
		return Objects.equals(this.classpaths, that.classpaths);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hashCode(this.classpaths);
	}
}
