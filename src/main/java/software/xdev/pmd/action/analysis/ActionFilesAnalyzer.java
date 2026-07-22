package software.xdev.pmd.action.analysis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.NotNull;

import com.intellij.concurrency.virtualThreads.IntelliJVirtualThreads;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import software.xdev.pmd.analysis.NoAnalysisReason;
import software.xdev.pmd.analysis.PMDAnalysisResult;
import software.xdev.pmd.analysis.PMDAnalyzer;
import software.xdev.pmd.analysis.PsiFileValidator;
import software.xdev.pmd.config.ConfigurationLocationSource;
import software.xdev.pmd.config.PluginConfiguration;
import software.xdev.pmd.config.PluginConfigurationManager;
import software.xdev.pmd.currentfile.CombinedPMDAnalysisResult;
import software.xdev.pmd.model.scope.ScanScope;
import software.xdev.pmd.ui.toolwindow.analysis.report.ReportViewManager;


public class ActionFilesAnalyzer
{
	private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(
		Math.max(Runtime.getRuntime().availableProcessors() - 1, 2),
		IntelliJVirtualThreads.ofVirtual()
			.name("PMDX-Parallel-Analysis-", 0)
			.factory());
	
	public void analyze(@NotNull final ReplayableAnalysisInfo ra)
	{
		final Project project = ra.getProject();
		if(project == null || project.isDisposed())
		{
			return;
		}
		
		final VirtualFile[] selectedFiles = ra.getFiles(project);
		if(selectedFiles == null || selectedFiles.length == 0)
		{
			return;
		}
		
		ProgressManager.getInstance().run(new Task.Backgroundable(project, "Analysing files...", true)
		{
			@Override
			public void run(@NotNull final ProgressIndicator indicator)
			{
				indicator.setIndeterminate(true);
				ActionFilesAnalyzer.this.analyzeAsync(project, indicator, selectedFiles, ra);
			}
		});
	}
	
	@SuppressWarnings("checkstyle:MagicNumber")
	private void analyzeAsync(
		final Project project,
		final ProgressIndicator progressIndicator,
		final VirtualFile[] selectedFiles,
		final ReplayableAnalysisInfo replayableAnalysisInfo)
	{
		progressIndicator.setText("Collecting files...");
		progressIndicator.setIndeterminate(true);
		
		final PsiManager psiManager = PsiManager.getInstance(project);
		final PluginConfiguration pluginConfiguration =
			project.getService(PluginConfigurationManager.class).getCurrent();
		final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
		
		final Map<Optional<com.intellij.openapi.module.Module>, Set<PsiFile>> psiFiles =
			ReadAction.computeBlocking(() -> this.collectFiles(
				projectFileIndex,
				psiManager,
				pluginConfiguration,
				progressIndicator,
				selectedFiles));
		
		if(psiFiles.isEmpty())
		{
			project.getService(ReportViewManager.class).displayNewReport(
				CombinedPMDAnalysisResult.combine(PMDAnalysisResult.empty(NoAnalysisReason.NO_APPLICABLE_FILES)),
				replayableAnalysisInfo);
			return;
		}
		progressIndicator.checkCanceled();
		progressIndicator.setText("Launching analysis");
		progressIndicator.setText2("");
		
		final List<CompletableFuture<PMDAnalysisResult>> cfs = psiFiles.entrySet()
			.stream()
			.map(e -> CompletableFuture.supplyAsync(
				() -> project.getService(PMDAnalyzer.class).analyze(
					e.getKey(),
					e.getValue(),
					false,
					project.getService(ConfigurationLocationSource.class)
						.getConfigurationLocations(e.getKey().orElse(null)),
					progressIndicator),
				EXECUTOR
			))
			.toList();
		
		try
		{
			CompletableFuture.allOf(cfs.toArray(CompletableFuture[]::new))
				.get(15, TimeUnit.MINUTES);
		}
		catch(final InterruptedException e)
		{
			progressIndicator.checkCanceled();
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Got interrupted", e);
		}
		catch(final ExecutionException e)
		{
			progressIndicator.checkCanceled();
			throw new IllegalStateException("Analysis execution failed", e);
		}
		catch(final TimeoutException e)
		{
			progressIndicator.checkCanceled();
			progressIndicator.cancel();
			throw new IllegalStateException("Analysis is taking too long", e);
		}
		
		final CombinedPMDAnalysisResult combined = CombinedPMDAnalysisResult.combine(cfs
			.stream()
			.map(CompletableFuture::join)
			.toList());
		
		project.getService(ReportViewManager.class).displayNewReport(combined, replayableAnalysisInfo);
	}
	
	@NotNull
	private Map<Optional<com.intellij.openapi.module.Module>, Set<PsiFile>> collectFiles(
		final ProjectFileIndex projectFileIndex,
		final PsiManager psiManager,
		final PluginConfiguration pluginConfiguration,
		final ProgressIndicator progressIndicator,
		final VirtualFile[] selectedFiles)
	{
		final Map<Optional<com.intellij.openapi.module.Module>, Set<PsiFile>> psiFiles = new HashMap<>();
		final AtomicInteger counterScanned = new AtomicInteger();
		final VirtualFileVisitor<Object> fileVisitor = new VirtualFileVisitor<>()
		{
			@Override
			public boolean visitFile(@NotNull final VirtualFile file)
			{
				progressIndicator.checkCanceled();
				progressIndicator.setText2(counterScanned.getAndIncrement() + "x elements checked");
				
				if(file.isDirectory())
				{
					final ScanScope scanScope = pluginConfiguration.scanScope();
					if(!scanScope.includeTestClasses()
						&& projectFileIndex.isInTestSourceContent(file))
					{
						return false;
					}
					
					return !projectFileIndex.isInGeneratedSources(file)
						&& !projectFileIndex.isExcluded(file)
						&& !projectFileIndex.isInLibrary(file);
				}
				
				final PsiFile psiFile = psiManager.findFile(file);
				
				final Optional<Module> optModule =
					Optional.ofNullable(ModuleUtilCore.findModuleForFile(psiFile));
				
				if(!PsiFileValidator.isScannable(psiFile, optModule, pluginConfiguration))
				{
					return false;
				}
				
				final Set<PsiFile> modulePsiFiles = psiFiles.computeIfAbsent(
					optModule,
					ignored -> new HashSet<>());
				modulePsiFiles.add(psiFile);
				return true;
			}
		};
		
		Arrays.stream(selectedFiles).forEach(file ->
			VfsUtilCore.visitChildrenRecursively(file, fileVisitor));
		return psiFiles;
	}
}
