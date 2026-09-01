package software.xdev.pmd.analysis.classloading;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * v7.26.0 of PMD's ClassPathClassLoader with minor cleanups.
 * <p>
 * Was re-created because the new version doesn't correctly iterate over parents.
 * </p>
 */
@SuppressWarnings("all")
public class FastClasspathClassLoader extends URLClassLoader implements JrtFindInFileSystem
{
	private static final Logger LOG = LoggerFactory.getLogger(FastClasspathClassLoader.class);
	
	private static final Path LIB_JRT_FS_JAR = Paths.get("lib", "jrt-fs.jar");
	
	private JrtFindInFileSystem jrtFindInFileSystem;
	
	static
	{
		registerAsParallelCapable();
		
		// Disable caching for jar files to prevent issues like #4899
		try
		{
			// Uses a pseudo URL to be able to call URLConnection#setDefaultUseCaches
			// with Java9+ there is a static method for that per protocol:
			// https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/net/URLConnection.html#setDefaultUseCaches(java.lang.String,boolean)
			URI.create("jar:file:file.jar!/").toURL().openConnection().setDefaultUseCaches(false);
		}
		catch(final IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public FastClasspathClassLoader(
		final Set<String> classpaths,
		final ClassLoader parent) throws IOException
	{
		super(new URL[0], parent);
		
		final List<Path> urlPaths = classpaths.stream()
			.map(Paths::get)
			.map(Path::toAbsolutePath)
			.toList();
		
		final List<Path> jrtPaths = urlPaths.stream()
			.filter(p -> p.endsWith(LIB_JRT_FS_JAR))
			.toList();
		
		this.jrtFindInFileSystem = determineJRTFindInFilesystem(jrtPaths, parent);
		
		urlPaths.stream()
			.filter(urlPath -> !jrtPaths.contains(urlPath))
			.map(urlPath -> {
				try
				{
					return urlPath.toUri().normalize().toURL();
				}
				catch(MalformedURLException mue)
				{
					throw new UncheckedIOException(mue);
				}
			})
			.forEach(this::addURL);
	}
	
	private static JrtFindInFileSystem determineJRTFindInFilesystem(final List<Path> jrtPaths, ClassLoader parent)
	{
		if(!jrtPaths.isEmpty())
		{
			if(jrtPaths.size() > 1)
			{
				throw new IllegalStateException("Multiple JRTs");
			}
			return new JrtResolver(jrtPaths.getFirst());
		}
		
		if(parent instanceof FastClasspathClassLoader icl && icl.jrtFindInFileSystem() != null)
		{
			return icl;
		}
		return null;
	}
	
	public JrtFindInFileSystem jrtFindInFileSystem()
	{
		return jrtFindInFileSystem;
	}
	
	@Override
	public Path findInFileSystem(final String name)
	{
		return jrtFindInFileSystem != null
			? jrtFindInFileSystem.findInFileSystem(name)
			: null;
	}
	
	@Override
	public String toString()
	{
		return this.getClass().getSimpleName()
			+ "[["
			+ StringUtils.join(this.getURLs(), ":")
			+ "] jrt-resolver: " + this.jrtFindInFileSystem + " parent: " + this.getParent() + ']';
	}
	
	static final String MODULE_INFO_SUFFIX = "module-info.class";
	private static final String MODULE_INFO_SUFFIX_SLASH = "/" + MODULE_INFO_SUFFIX;
	// this is lazily initialized on first query of a module-info.class
	private Map<String, URL> moduleNameToModuleInfoUrls;
	
	static String extractModuleName(final String name)
	{
		return name.endsWith(MODULE_INFO_SUFFIX_SLASH)
			? name.substring(0, name.length() - MODULE_INFO_SUFFIX_SLASH.length())
			: null;
	}
	
	@Override
	public InputStream getResourceAsStream(final String name)
	{
		// always first search in jrt-fs, if available
		// note: we can't override just getResource(String) and return a jrt:/-URL, because the URL itself
		// won't be connected to the correct JrtFileSystem and would just load using the system classloader.
		final Path candidate = findInFileSystem(name);
		if(candidate != null)
		{
			return newInputStreamFromJrtFilesystem(candidate);
		}
		
		// search in the other jars of the aux classpath.
		// this will call this.getResource, which will do a child-first search, see below.
		return super.getResourceAsStream(name);
	}
	
	static InputStream newInputStreamFromJrtFilesystem(final Path path)
	{
		LOG.trace("Found {}", path);
		try
		{
			// Note: The input streams from JrtFileSystem are ByteArrayInputStreams and do not
			// need to be closed - we don't need to track these. The filesystem itself needs to be closed at the end.
			// See https://github.com/openjdk/jdk/blob/970cd202049f592946f9c1004ea92dbd58abf6fb/src/java.base/share/classes/jdk/internal/jrtfs/JrtFileSystem.java#L334
			return Files.newInputStream(path);
		}
		catch(final IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}
	
	private static class ModuleNameExtractor extends ClassVisitor
	{
		private String moduleName;
		
		protected ModuleNameExtractor()
		{
			super(Opcodes.ASM9);
		}
		
		@Override
		public ModuleVisitor visitModule(final String name, final int access, final String version)
		{
			this.moduleName = name;
			return null;
		}
		
		public String getModuleName()
		{
			return this.moduleName;
		}
	}
	
	private void collectAllModules()
	{
		if(this.moduleNameToModuleInfoUrls != null)
		{
			return;
		}
		
		final Map<String, URL> allModules = new HashMap<>();
		try
		{
			this.collectModules(allModules, this.findResources(MODULE_INFO_SUFFIX));
			
			// also search in parents
			this.collectModules(allModules, this.getParent().getResources(MODULE_INFO_SUFFIX));
			
			LOG.debug("Found {} modules on auxclasspath", allModules.size());
			
			this.moduleNameToModuleInfoUrls = Collections.unmodifiableMap(allModules);
		}
		catch(final IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private void collectModules(final Map<String, URL> allModules, final Enumeration<URL> moduleInfoUrls)
		throws IOException
	{
		while(moduleInfoUrls.hasMoreElements())
		{
			final URL url = moduleInfoUrls.nextElement();
			
			final ModuleNameExtractor finder = new ModuleNameExtractor();
			try(final InputStream inputStream = url.openStream())
			{
				final ClassReader classReader = new ClassReader(inputStream);
				classReader.accept(finder, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
			}
			allModules.putIfAbsent(finder.getModuleName(), url);
		}
	}
	
	@Override
	public URL getResource(final String name)
	{
		// Override to make it child-first. This is the method used by
		// pmd-java's type resolution to fetch classes, instead of loadClass.
		Objects.requireNonNull(name);
		
		final Path candidate = findInFileSystem(name);
		if(candidate != null)
		{
			try
			{
				return candidate.toUri().toURL();
			}
			catch(MalformedURLException e)
			{
				throw new UncheckedIOException(e);
			}
		}
		
		final String moduleName = extractModuleName(name);
		if(moduleName != null)
		{
			this.collectAllModules();
			assert this.moduleNameToModuleInfoUrls != null
				: "Modules should have been detected by collectAllModules()";
			return this.moduleNameToModuleInfoUrls.get(moduleName);
		}
		
		final URL url = this.findResource(name);
		
		if(url == null)
		{
			// Only search in parent if it is present
			// The original code called super.getResource which executed findResource again (already executed above)
			// when nothing was returned by the parent
			final ClassLoader parent = getParent();
			if(parent != null)
			{
				return parent.getResource(name);
			}
		}
		return url;
	}
	
	@Override
	protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException
	{
		throw new IllegalStateException("This classloader shouldn't be used to load classes");
	}
	
	@Override
	public void close() throws IOException
	{
		if(this.jrtFindInFileSystem != null)
		{
			if(jrtFindInFileSystem instanceof JrtResolver jrtResolver)
			{
				jrtResolver.close();
			}
			this.jrtFindInFileSystem = null;
		}
		super.close();
	}
}
