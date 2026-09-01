package software.xdev.pmd.analysis;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.intellij.concurrency.virtualThreads.IntelliJVirtualThreads;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.psi.PsiFile;
import com.intellij.util.PathsList;

import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.document.TextFile;
import net.sourceforge.pmd.lang.rule.RuleSet;
import net.sourceforge.pmd.reporting.FileAnalysisListener;
import net.sourceforge.pmd.reporting.GlobalAnalysisListener;
import net.sourceforge.pmd.reporting.Report;
import software.xdev.pmd.analysis.pmd.FastClasspathClassLoader;
import software.xdev.pmd.analysis.pmd.NonCrashingPMDConfiguration;
import software.xdev.pmd.analysis.validate.PsiFileValidator;
import software.xdev.pmd.config.PluginConfiguration;
import software.xdev.pmd.config.PluginConfigurationManager;
import software.xdev.pmd.external.org.springframework.util.ConcurrentReferenceHashMap;
import software.xdev.pmd.langversion.ManagedLanguageVersionResolver;
import software.xdev.pmd.model.config.rulesetlocation.ConfigurationLocation;


@SuppressWarnings("deprecation")
public class PMDAnalyzer implements Disposable
{
	private static final Logger LOG = Logger.getInstance(PMDAnalyzer.class);
	
	@SuppressWarnings("UnstableApiUsage")
	private static final ExecutorService RULESET_LOADER_SERVICE = Executors.newThreadPerTaskExecutor(
		IntelliJVirtualThreads.ofVirtual()
			.name("PMDX-RuleSetLoader-", 0)
			.factory());
	
	private final Project project;
	
	private final Map<Optional<Module>, ReentrantLock> locks = new ConcurrentHashMap<>();
	private final Map<Optional<Module>, CacheFile> cacheFiles = new ConcurrentHashMap<>();
	
	private final Map<Set<String>, Set<String>> cachedComputedPMDSDKClassPaths = new ConcurrentReferenceHashMap<>();
	// NOTE: Classloading/caching in modules uses layers (for caching):
	// * SDK/JDK
	// * Libs
	// * App-Classes (can't be cached)
	private final Map<Set<String>, SdkClassLoaderCache> cachedSdkLibAuxClassLoaders =
		new ConcurrentReferenceHashMap<>();
	
	
	record SdkClassLoaderCache(
		ClassLoader classLoader,
		Map<Set<String>, ClassLoader> libClassLoaders)
	{
		SdkClassLoaderCache(final ClassLoader cl)
		{
			this(cl, new ConcurrentHashMap<>());
		}
	}
	
	public PMDAnalyzer(final Project project)
	{
		this.project = project;
	}
	
	private String cacheFile(final Optional<Module> optModule)
	{
		return this.cacheFiles.computeIfAbsent(
				optModule,
				ignored -> {
					try
					{
						final Path path = Files.createTempFile("pmd-intellij-cache", ".cache");
						return new CacheFile(path, path.toAbsolutePath().toString());
					}
					catch(final IOException e)
					{
						throw new UncheckedIOException(e);
					}
				})
			.absolutePath();
	}
	
	public PMDAnalysisResult analyze(
		final Optional<Module> optModule,
		final Set<PsiFile> filesToScan,
		final boolean determineIfFilesApplicable,
		final Collection<ConfigurationLocation> configurationLocations,
		final ProgressIndicator progressIndicator)
	{
		if(filesToScan.isEmpty())
		{
			return PMDAnalysisResult.empty(NoAnalysisReason.NO_FILES);
		}
		if(configurationLocations.isEmpty())
		{
			return PMDAnalysisResult.empty(NoAnalysisReason.NO_CONFIG_LOCATION_OR_EXCLUDED);
		}
		
		final ReentrantLock lock = this.locks.computeIfAbsent(optModule, ignored -> new ReentrantLock());
		lock.lock();
		
		try
		{
			return this.analyzeInternal(
				optModule,
				filesToScan,
				determineIfFilesApplicable,
				configurationLocations,
				progressIndicator);
		}
		finally
		{
			lock.unlock();
		}
	}
	
	private PMDAnalysisResult analyzeInternal(
		final Optional<Module> optModule,
		final Set<PsiFile> filesToScan,
		final boolean determineIfFilesApplicable,
		final Collection<ConfigurationLocation> configurationLocations,
		final ProgressIndicator progressIndicator)
	{
		final long startMs = System.currentTimeMillis();
		
		final ClassLoader baseRulesetClassLoader =
			this.project.getService(ProjectRulesetClasspathManager.class).getClassLoader();
		
		// Load ruleset (if required) & async in background
		final CompletableFuture<List<RuleSet>> cfLoadRuleSetsAsync = CompletableFuture.supplyAsync(
				() -> configurationLocations.stream()
					.map(configLoc -> configLoc.getOrRefreshCachedRuleSet(baseRulesetClassLoader))
					.filter(Objects::nonNull)
					.toList(),
			RULESET_LOADER_SERVICE);
		
		final PluginConfiguration pluginConfiguration =
			this.project.getService(PluginConfigurationManager.class).getCurrent();
		
		final Collection<PsiFile> applicableFiles = determineIfFilesApplicable
			? this.determineApplicableFiles(optModule, filesToScan, pluginConfiguration, progressIndicator)
			: filesToScan;
		if(applicableFiles.isEmpty())
		{
			cfLoadRuleSetsAsync.cancel(false);
			return PMDAnalysisResult.empty(NoAnalysisReason.NO_APPLICABLE_FILES);
		}
		
		progressIndicator.checkCanceled();
		progressIndicator.setText("Calculating languages and version");
		progressIndicator.setIndeterminate(true);
		
		final Map<LanguageVersion, Set<PsiFile>> highestLanguageVersionAndFiles =
			this.getHighestLanguageVersionAndFiles(this.groupPsiFilesBySupportedLanguageAndVersion(applicableFiles));
		
		progressIndicator.checkCanceled();
		progressIndicator.setText("Preparing configuration");
		
		final PMDConfiguration pmdConfig = new NonCrashingPMDConfiguration();
		pmdConfig.setDefaultLanguageVersions(highestLanguageVersionAndFiles.keySet().stream().toList());
		
		final List<Module> modules = optModule
			.map(List::of)
			.orElseGet(() -> List.of(ModuleManager.getInstance(this.project).getModules()));
		
		// Compared to pmdConfig#setAuxClasspath this is around ~50% faster because
		// we can cache the individual class loaders for Libs and SDK while PMD will always re-create them
		pmdConfig.setClassLoader(this.classLoaderFor(modules));
		
		if(pluginConfiguration.showSuppressedWarnings())
		{
			pmdConfig.setShowSuppressedViolations(true);
		}
		if(pluginConfiguration.useSingleThread())
		{
			pmdConfig.setThreads(-1);
		}
		if(pluginConfiguration.useCacheFile())
		{
			pmdConfig.setAnalysisCacheLocation(this.cacheFile(optModule));
		}
		
		progressIndicator.setText("Preparing files for scan");
		
		final List<IDETextFile> ideFiles = highestLanguageVersionAndFiles.entrySet()
			.stream()
			.flatMap(e -> e.getValue().stream().map(f -> new IDETextFile(e.getKey(), f)))
			.toList();
		
		final Report report;
		try(final PmdAnalysis pmd = PmdAnalysis.create(pmdConfig))
		{
			// Prevent ruleset parsing
			pmd.addRuleSets(cfLoadRuleSetsAsync.join());
			
			ideFiles.forEach(pmd.files()::addFile);
			
			progressIndicator.checkCanceled();
			progressIndicator.setText("Analysing");
			progressIndicator.setFraction(0);
			
			pmd.addListener(new ProgressReportingAnalysisListener(progressIndicator, ideFiles.size()));
			
			report = pmd.performAnalysisAndCollectReport();
		}
		
		progressIndicator.setText("Finishing analysis");
		progressIndicator.setText2("");
		progressIndicator.setIndeterminate(true);
		
		final PMDAnalysisResult result = new PMDAnalysisResult(
			report,
			ideFiles.stream()
				.filter(IDETextFile::hasFileId)
				.collect(Collectors.toMap(
					IDETextFile::getFileIdIfPresent,
					IDETextFile::getPsiFile)));
		
		LOG.info("Analysis took " + (System.currentTimeMillis() - startMs) + "ms");
		
		return result;
	}
	
	static class ProgressReportingAnalysisListener implements GlobalAnalysisListener
	{
		private final AtomicInteger counter = new AtomicInteger(0);
		private final ProgressIndicator progressIndicator;
		private final int totalFiles;
		
		ProgressReportingAnalysisListener(final ProgressIndicator progressIndicator, final int totalFiles)
		{
			this.progressIndicator = progressIndicator;
			this.totalFiles = totalFiles;
		}
		
		@Override
		public FileAnalysisListener startFileAnalysis(final TextFile file)
		{
			this.progressIndicator.setFraction((double)this.counter.incrementAndGet() / this.totalFiles);
			this.progressIndicator.setText2(((IDETextFile)file).getPsiFile().getName());
			return FileAnalysisListener.noop();
		}
		
		@Override
		public void close()
		{
			// Nothing
		}
	}
	
	@NotNull
	private List<PsiFile> determineApplicableFiles(
		final Optional<Module> optModule,
		final Set<PsiFile> filesToScan,
		final PluginConfiguration pluginConfiguration,
		final ProgressIndicator progressIndicator)
	{
		progressIndicator.setText("Determining files for scan");
		progressIndicator.setIndeterminate(false);
		progressIndicator.setFraction(0);
		
		final int totalFiles = filesToScan.size();
		final AtomicInteger counter = new AtomicInteger(0);
		
		final PsiFileValidator psiFileValidator = this.project.getService(PsiFileValidator.class);
		
		final List<PsiFile> files = ReadAction.computeBlocking(() -> filesToScan.stream()
			.filter(file -> {
				progressIndicator.setFraction((double)counter.incrementAndGet() / totalFiles);
				progressIndicator.setText2(file != null ? file.getName() : null);
				
				return psiFileValidator.isScannable(
					file,
					optModule,
					pluginConfiguration);
			})
			.toList());
		
		progressIndicator.setText2("");
		
		return files;
	}
	
	private Map<Language, Map<LanguageVersion, List<PsiFile>>> groupPsiFilesBySupportedLanguageAndVersion(
		final Collection<PsiFile> files)
	{
		final ManagedLanguageVersionResolver resolver = new ManagedLanguageVersionResolver();
		
		return files.stream()
			.collect(Collectors.groupingBy(resolver::resolveLanguage))
			.entrySet()
			.stream()
			.filter(e -> e.getKey().isPresent())
			.collect(Collectors.groupingBy(
				e -> e.getKey().orElseThrow().getLanguage(),
				Collectors.toMap(e -> e.getKey().orElseThrow(), Map.Entry::getValue)));
	}
	
	private Map<LanguageVersion, Set<PsiFile>> getHighestLanguageVersionAndFiles(
		final Map<Language, Map<LanguageVersion, List<PsiFile>>> groupPsiFilesByLanguageAndVersion)
	{
		return groupPsiFilesByLanguageAndVersion.entrySet()
			.stream()
			.collect(Collectors.toMap(
				e -> e.getValue()
					.keySet()
					.stream()
					.max(LanguageVersion::compareTo)
					.orElseThrow(),
				e -> e.getValue()
					.values()
					.stream()
					.flatMap(Collection::stream)
					.collect(Collectors.toSet())));
	}
	
	@NotNull
	private ClassLoader classLoaderFor(final List<Module> modules)
	{
		final Set<String> nonSDKPaths = this.classPathFor(modules, OrderEnumerator::withoutSdk);
		final Set<String> appOnlyClassPaths = this.classPathFor(modules, o -> o.withoutSdk().withoutLibraries());
		final Set<String> libClassPaths = nonSDKPaths.stream()
			.filter(s -> !appOnlyClassPaths.contains(s))
			.collect(Collectors.toCollection(LinkedHashSet::new));
		
		// SDK
		final Set<String> sdkClassPathsIDE = this.classPathFor(modules, OrderEnumerator::sdkOnly);
		final Set<String> sdkClassPathsForPmd =
			this.cachedComputedPMDSDKClassPaths.computeIfAbsent(sdkClassPathsIDE, this::computePMDSdkClassPaths);
		
		final SdkClassLoaderCache sdkClassLoaderData = this.cachedSdkLibAuxClassLoaders.computeIfAbsent(
			sdkClassPathsForPmd,
			sdkPaths -> new SdkClassLoaderCache(this.createClasspathClassLoader(
				sdkPaths,
				null)));
		
		// App-Only
		return this.createClasspathClassLoader(
			appOnlyClassPaths,
			// Lib
			sdkClassLoaderData.libClassLoaders().computeIfAbsent(
				libClassPaths,
				libPaths -> this.createClasspathClassLoader(
					libPaths,
					sdkClassLoaderData.classLoader())));
	}
	
	private Set<String> computePMDSdkClassPaths(final Set<String> sdkClassPathsIDE)
	{
		// The IDE returned paths look like this:
		// C:\.jdks\java25!\java.base
		// C:\.jdks\java25!\java.sql
		// ...
		
		// These need to be deduplicated to read
		// C:\.jdks\java25\lib\jrt-fs.jar
		// or PMD will not detect it
		
		// If there are multiple JDKs one MUST be picked
		
		final String javaBaseEnding = "!" + File.separator + "java.base";
		final List<String> sdkBasePaths = sdkClassPathsIDE.stream()
			.filter(s -> s.endsWith(javaBaseEnding))
			.map(s -> s.substring(0, s.indexOf('!')))
			.toList();
		
		final AtomicBoolean alreadyReplacedBasePath = new AtomicBoolean(false);
		return sdkClassPathsIDE.stream()
			.map(s -> sdkBasePaths.stream()
				.filter(basePath -> s.startsWith(basePath + "!"))
				.findFirst()
				.map(basePath -> alreadyReplacedBasePath.compareAndSet(false, true)
					? Optional.of(basePath + File.separator + "lib" + File.separator + "jrt-fs.jar")
					: Optional.<String>empty())
				.orElseGet(() -> Optional.of(s)))
			.filter(Optional::isPresent)
			.map(Optional::orElseThrow)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}
	
	private Set<String> classPathFor(
		final Collection<Module> modules,
		final UnaryOperator<OrderEnumerator> mapOrderEnumerator)
	{
		return modules.stream()
			.map(OrderEnumerator::orderEntries)
			.map(OrderEnumerator::recursively)
			.map(mapOrderEnumerator)
			.map(OrderEnumerator::getPathsList)
			.map(PathsList::getPathList)
			.flatMap(Collection::stream)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}
	
	private ClassLoader createClasspathClassLoader(
		final Set<String> classPaths,
		final ClassLoader parentLoader)
	{
		try
		{
			return new FastClasspathClassLoader(classPaths, parentLoader);
		}
		catch(final IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}
	
	@Override
	public void dispose()
	{
		this.cacheFiles.values()
			.stream()
			.map(CacheFile::path)
			.forEach(f -> {
				try
				{
					Files.deleteIfExists(f);
				}
				catch(final IOException ioe)
				{
					LOG.warn("Failed to delete cache file", ioe);
				}
			});
		this.cacheFiles.clear();
	}
	
	record CacheFile(
		Path path,
		String absolutePath)
	{
	}
}
