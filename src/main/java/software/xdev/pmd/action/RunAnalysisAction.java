package software.xdev.pmd.action;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import software.xdev.pmd.analysis.PMDAnalyzer;
import software.xdev.pmd.analysis.PsiFileValidator;
import software.xdev.pmd.config.ConfigurationLocationSource;
import software.xdev.pmd.config.PluginConfiguration;
import software.xdev.pmd.config.PluginConfigurationManager;
import software.xdev.pmd.currentfile.CombinedPMDAnalysisResult;
import software.xdev.pmd.model.scope.ScanScope;
import software.xdev.pmd.ui.toolwindow.analysis.report.ReportViewManager;


public class RunAnalysisAction extends AbstractAnAction
{
	@Override
	protected boolean isVisible(final AnActionEvent ev, final Project project)
	{
		final VirtualFile[] files = ev.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
		return files != null && files.length > 0;
	}
	
	@Override
	public void actionPerformed(@NotNull final AnActionEvent ev)
	{
		final Project project = ev.getData(CommonDataKeys.PROJECT);
		if(project == null)
		{
			return;
		}
		
		final VirtualFile[] selectedFiles = ActionPlaces.MAIN_MENU.equals(ev.getPlace())
			? VfsUtil.getCommonAncestors(ProjectRootManager.getInstance(project).getContentRoots())
			: ev.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
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
				RunAnalysisAction.this.analyze(project, indicator, selectedFiles);
			}
		});
	}
	
	@SuppressWarnings("checkstyle:MagicNumber")
	private void analyze(
		final Project project,
		final ProgressIndicator progressIndicator,
		final VirtualFile[] selectedFiles)
	{
		progressIndicator.setText("Collecting files...");
		progressIndicator.setIndeterminate(true);
		
		final PsiManager psiManager = PsiManager.getInstance(project);
		final PluginConfiguration pluginConfiguration =
			project.getService(PluginConfigurationManager.class).getCurrent();
		final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
		
		final Map<Optional<Module>, Set<PsiFile>> psiFiles = ReadAction.compute(() ->
			this.collectFiles(projectFileIndex, psiManager, pluginConfiguration, progressIndicator, selectedFiles));
		
		if(psiFiles.isEmpty())
		{
			return;
		}
		progressIndicator.checkCanceled();
		progressIndicator.setText("Launching analyses");
		progressIndicator.setText2("");
		
		final CombinedPMDAnalysisResult combined = CombinedPMDAnalysisResult.combine(psiFiles.entrySet()
			.parallelStream()
			.map(e ->
				project.getService(PMDAnalyzer.class)
					.analyze(
						e.getKey(),
						e.getValue(),
						false,
						project.getService(ConfigurationLocationSource.class)
							.getConfigurationLocations(e.getKey().orElse(null)),
						progressIndicator))
			.toList());
		
		project.getService(ReportViewManager.class).displayNewReport(combined);
	}
	
	@NotNull
	private Map<Optional<Module>, Set<PsiFile>> collectFiles(
		final ProjectFileIndex projectFileIndex,
		final PsiManager psiManager,
		final PluginConfiguration pluginConfiguration,
		final ProgressIndicator progressIndicator,
		final VirtualFile[] selectedFiles)
	{
		final Map<Optional<Module>, Set<PsiFile>> psiFiles = new HashMap<>();
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
