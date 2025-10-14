package software.xdev.pmd.analysis;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
import com.intellij.util.containers.BidirectionalMap;

import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.document.TextFile;
import net.sourceforge.pmd.reporting.FileAnalysisListener;
import net.sourceforge.pmd.reporting.GlobalAnalysisListener;
import net.sourceforge.pmd.reporting.Report;
import software.xdev.pmd.config.PluginConfigurationManager;
import software.xdev.pmd.langversion.ManagedLanguageVersionResolver;
import software.xdev.pmd.model.config.ConfigurationLocation;


public class PMDAnalyzer implements Disposable
{
	private static final Logger LOG = Logger.getInstance(PMDAnalyzer.class);
	
	private final Project project;
	
	private Path cacheFilePath;
	private String cacheFile;
	
	public PMDAnalyzer(final Project project)
	{
		this.project = project;
	}
	
	private String cacheFile()
	{
		if(this.cacheFile == null)
		{
			try
			{
				this.cacheFilePath = Files.createTempFile("pmd-intellij-cache", ".cache");
				this.cacheFile = this.cacheFilePath.toAbsolutePath().toString();
			}
			catch(final IOException ioex)
			{
				throw new UncheckedIOException(ioex);
			}
		}
		return this.cacheFile;
	}
	
	public PMDAnalysisResult analyze(
		final Optional<Module> optModule,
		final Set<PsiFile> filesToScan,
		final Collection<ConfigurationLocation> configurationLocations,
		final ProgressIndicator progressIndicator)
	{
		if(filesToScan.isEmpty())
		{
			throw new IllegalArgumentException("No files to scan");
		}
		
		final long startMs = System.currentTimeMillis();
		
		final List<PsiFile> applicableFiles = this.determineApplicableFiles(optModule, filesToScan, progressIndicator);
		
		progressIndicator.checkCanceled();
		progressIndicator.setText("Calculating languages and version");
		progressIndicator.setIndeterminate(true);
		
		final Map<LanguageVersion, Set<PsiFile>> highestLanguageVersionAndFiles =
			this.getHighestLanguageVersionAndFiles(this.groupPsiFilesBySupportedLanguageAndVersion(applicableFiles));
		
		progressIndicator.checkCanceled();
		progressIndicator.setText("Preparing configuration");
		
		final PMDConfiguration pmdConfig = new PMDConfiguration();
		pmdConfig.setDefaultLanguageVersions(highestLanguageVersionAndFiles.keySet().stream().toList());
		pmdConfig.prependAuxClasspath(this.getFullClassPathFor(optModule
			.map(List::of) // TODO Maybe resolve "dependency" modules
			.orElseGet(() -> List.of(ModuleManager.getInstance(this.project).getModules()))));
		pmdConfig.setRuleSets(configurationLocations.stream()
			.map(ConfigurationLocation::getLocation)
			.toList());
		// TODO config
		pmdConfig.setAnalysisCacheLocation(this.cacheFile());
		// TODO Thread config
		// TODO Suppressed
		
		progressIndicator.setText("Preparing files for scan");
		
		final List<IDETextFile> ideFiles = highestLanguageVersionAndFiles.entrySet()
			.stream()
			.flatMap(e -> e.getValue().stream().map(f -> new IDETextFile(e.getKey(), f)))
			.toList();
		
		final Report report;
		try(final PmdAnalysis pmd = PmdAnalysis.create(pmdConfig))
		{
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
					IDETextFile::getPsiFile,
					(l, r) -> r,
					BidirectionalMap::new)));
		
		LOG.warn("Analysis took " + (System.currentTimeMillis() - startMs) + "ms");
		
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
					this.project.getService(PluginConfigurationManager.class));
			})
			.toList());
		
		progressIndicator.setText2("");
		
		return files;
	}
	
	private Map<Language, Map<LanguageVersion, List<PsiFile>>> groupPsiFilesBySupportedLanguageAndVersion(
		final List<PsiFile> files)
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
	
	private String getFullClassPathFor(final Collection<Module> modules)
	{
		return modules.stream()
			.map(OrderEnumerator::orderEntries)
			.map(OrderEnumerator::recursively)
			.map(OrderEnumerator::getPathsList)
			.map(PathsList::getPathList)
			.flatMap(Collection::stream)
			.distinct()
			.collect(Collectors.joining(File.pathSeparator));
	}
	
	@Override
	public void dispose()
	{
		if(this.cacheFilePath != null)
		{
			try
			{
				Files.deleteIfExists(this.cacheFilePath);
			}
			catch(final IOException ioe)
			{
				LOG.warn("Failed to delete cache file", ioe);
			}
			this.cacheFilePath = null;
		}
		this.cacheFile = null;
	}
}
