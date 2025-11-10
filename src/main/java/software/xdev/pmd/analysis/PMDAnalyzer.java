package software.xdev.pmd.analysis;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

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
import net.sourceforge.pmd.internal.util.ClasspathClassLoader;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.document.TextFile;
import net.sourceforge.pmd.lang.rule.RuleSet;
import net.sourceforge.pmd.reporting.FileAnalysisListener;
import net.sourceforge.pmd.reporting.GlobalAnalysisListener;
import net.sourceforge.pmd.reporting.Report;
import software.xdev.pmd.config.PluginConfiguration;
import software.xdev.pmd.config.PluginConfigurationManager;
import software.xdev.pmd.external.org.apache.shiro.lang.util.SoftHashMap;
import software.xdev.pmd.langversion.ManagedLanguageVersionResolver;
import software.xdev.pmd.model.config.ConfigurationLocation;


public class PMDAnalyzer implements Disposable
{
	private static final Logger LOG = Logger.getInstance(PMDAnalyzer.class);
	
	private static final ExecutorService RULESET_LOADER_SERVICE = Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
		.name("RuleSetLoader", 0)
		.factory());
	
	private final Project project;
	
	private final Map<Optional<Module>, ReentrantLock> locks = Collections.synchronizedMap(new HashMap<>());
	private final Map<Optional<Module>, CacheFile> cacheFiles = Collections.synchronizedMap(new HashMap<>());
	// Reuse classloader when path is the same
	private final Map<Set<String>, ClassLoader> cachedSdkLibAuxClassLoader =
		Collections.synchronizedMap(new SoftHashMap<>());
	
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
			return PMDAnalysisResult.empty();
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
		
		// Load ruleset - if required - async in background
		final CompletableFuture<List<RuleSet>> cfLoadRuleSetsAsync =
			CompletableFuture.supplyAsync(
				() -> configurationLocations.stream()
					.map(ConfigurationLocation::getOrRefreshCachedRuleSet)
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
			return PMDAnalysisResult.empty();
		}
		
		progressIndicator.checkCanceled();
		progressIndicator.setText("Calculating languages and version");
		progressIndicator.setIndeterminate(true);
		
		final Map<LanguageVersion, Set<PsiFile>> highestLanguageVersionAndFiles =
			this.getHighestLanguageVersionAndFiles(this.groupPsiFilesBySupportedLanguageAndVersion(applicableFiles));
		
		progressIndicator.checkCanceled();
		progressIndicator.setText("Preparing configuration");
		
		final PMDConfiguration pmdConfig = new PMDConfiguration();
		pmdConfig.setDefaultLanguageVersions(highestLanguageVersionAndFiles.keySet().stream().toList());
		
		final List<Module> modules = optModule
			.map(List::of)
			.orElseGet(() -> List.of(ModuleManager.getInstance(this.project).getModules()));
		
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
		
		public ProgressReportingAnalysisListener(final ProgressIndicator progressIndicator, final int totalFiles)
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
		
		final List<PsiFile> files = ReadAction.compute(() -> filesToScan.stream()
			.filter(file -> {
				progressIndicator.setFraction((double)counter.incrementAndGet() / totalFiles);
				progressIndicator.setText2(file != null ? file.getName() : null);
				
				return PsiFileValidator.isScannable(
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
	private ClasspathClassLoader classLoaderFor(final List<Module> modules)
	{
		final Set<String> fullClassPaths = this.classPathFor(modules, UnaryOperator.identity());
		final Set<String> appClassPaths = this.classPathFor(modules, o -> o.withoutSdk().withoutLibraries());
		final Set<String> sdkLibClassPaths = fullClassPaths.stream()
			.filter(s -> !appClassPaths.contains(s))
			.collect(Collectors.toSet());
		
		return this.createClasspathClassLoader(
			appClassPaths,
			this.cachedSdkLibAuxClassLoader.computeIfAbsent(
				sdkLibClassPaths,
				paths -> this.createClasspathClassLoader(paths, PMDConfiguration.class.getClassLoader())));
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
			.collect(Collectors.toSet());
	}
	
	private ClasspathClassLoader createClasspathClassLoader(
		final Set<String> classPaths,
		final ClassLoader parentLoader)
	{
		try
		{
			return new ClasspathClassLoader(String.join(File.pathSeparator, classPaths), parentLoader);
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
