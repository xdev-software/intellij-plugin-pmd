package software.xdev.pmd.analysis.pmd;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.pmd.external.org.springframework.util.ConcurrentReferenceHashMap;


/**
 * Resolves Java RunTime classes
 */
public class JrtResolver implements AutoCloseable, JrtFindInFileSystem
{
	private static final Logger LOG = LoggerFactory.getLogger(JrtResolver.class);
	
	private final String javaHome;
	
	private final FileSystem fileSystem;
	private final Map<String, Set<String>> packagesDirsToModules;
	
	// Values need to be nullable!
	private final Map<String, Path> nameToCandidateCache = new ConcurrentReferenceHashMap<>(512);
	
	/**
	 * Initializes a Java Runtime Filesystem that will be used to load class files. This allows end users to provide in
	 * the aux classpath another Java Runtime version than the one used for executing PMD.
	 *
	 * @param filePath path to the file "lib/jrt-fs.jar" inside the java installation directory.
	 * @see <a href="https://openjdk.org/jeps/220">JEP 220: Modular Run-Time Images</a>
	 */
	public JrtResolver(final Path filePath)
	{
		try
		{
			LOG.debug("Detected Java Runtime Filesystem Provider in {}", filePath);
			
			if(filePath.getNameCount() < 2)
			{
				throw new IllegalArgumentException(
					"Can't determine java home from " + filePath + " - please provide a complete path.");
			}
			
			try(final URLClassLoader loader = new URLClassLoader(new URL[]{filePath.toUri().toURL()}))
			{
				final Map<String, String> env = new HashMap<>();
				// note: providing java.home here is crucial, so that the correct runtime image is loaded.
				// the class loader is only used to provide an implementation of JrtFileSystemProvider, if the current
				// Java runtime doesn't provide one (e.g. if running in Java 8).
				this.javaHome = filePath.getParent().getParent().toString();
				env.put("java.home", this.javaHome);
				LOG.debug("Creating jrt-fs with env {}", env);
				this.fileSystem = FileSystems.newFileSystem(URI.create("jrt:/"), env, loader);
			}
			
			this.packagesDirsToModules = new HashMap<>();
			final Path packages = this.fileSystem.getPath("packages");
			try(final Stream<Path> packagesStream = Files.list(packages))
			{
				packagesStream.forEach(p -> {
					final String packageName = p.getFileName().toString().replace('.', '/');
					try(final Stream<Path> modulesStream = Files.list(p))
					{
						final Set<String> modules = modulesStream
							.map(Path::getFileName)
							.map(Path::toString)
							.collect(Collectors.toSet());
						this.packagesDirsToModules.put(packageName, modules);
					}
					catch(final IOException e)
					{
						throw new UncheckedIOException(e);
					}
				});
			}
		}
		catch(final IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}
	
	@Override
	public Path findInFileSystem(final String name)
	{
		return this.nameToCandidateCache.computeIfAbsent(name, this::lookupInFileSystem);
	}
	
	private Path lookupInFileSystem(final String name)
	{
		final String moduleName = FastClasspathClassLoader.extractModuleName(name);
		if(moduleName != null)
		{
			LOG.trace("Trying to load module-info.class for module {} in jrt-fs", moduleName);
			final Path candidate =
				this.fileSystem.getPath("modules", moduleName, FastClasspathClassLoader.MODULE_INFO_SUFFIX);
			if(Files.exists(candidate))
			{
				return candidate;
			}
		}
		
		final int lastSlash = name.lastIndexOf('/');
		final String packageName = name.substring(0, Math.max(lastSlash, 0));
		final Set<String> moduleNames = this.packagesDirsToModules.get(packageName);
		if(moduleNames != null)
		{
			LOG.trace(
				"Trying to find {} in jrt-fs with packageName={} and modules={}",
				name, packageName, moduleNames);
			
			for(final String moduleCandidate : moduleNames)
			{
				final Path candidate = this.fileSystem.getPath("modules", moduleCandidate, name);
				if(Files.exists(candidate))
				{
					return candidate;
				}
			}
		}
		
		return null;
	}
	
	@Override
	public void close() throws IOException
	{
		this.fileSystem.close();
		// jrt created an own classloader to load the JrtFileSystemProvider class out of the
		// jrt-fs.jar. This needs to be closed manually.
		final ClassLoader classLoader = this.fileSystem.getClass().getClassLoader();
		if(classLoader instanceof final URLClassLoader urlClassLoader)
		{
			urlClassLoader.close();
		}
		this.nameToCandidateCache.clear();
	}
	
	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + " - " + this.javaHome;
	}
}
