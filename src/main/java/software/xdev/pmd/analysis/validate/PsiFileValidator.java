package software.xdev.pmd.analysis.validate;

import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.JavaProjectRootsUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import software.xdev.pmd.config.PluginConfiguration;
import software.xdev.pmd.external.org.springframework.util.ConcurrentReferenceHashMap;
import software.xdev.pmd.langversion.LanguageVersionResolverService;


public class PsiFileValidator implements Disposable
{
	private final Map<PluginConfiguration, Map<String, Boolean>> pluginConfigFileExclusionCache =
		new ConcurrentReferenceHashMap<>(ConcurrentReferenceHashMap.ReferenceType.WEAK);
	
	public boolean isScannable(
		@Nullable final PsiFile psiFile,
		@NotNull final Optional<Module> optModule,
		@NotNull final PluginConfiguration pluginConfig)
	{
		return psiFile != null
			&& psiFile.isValid()
			&& psiFile.isPhysical()
			&& this.isInSource(psiFile)
			&& this.hasDocument(psiFile)
			&& this.isValidFileType(psiFile, pluginConfig)
			&& this.isScannableIfTest(psiFile, pluginConfig)
			&& this.modulesMatch(psiFile, optModule)
			&& !this.isGenerated(psiFile)
			&& !this.isExcluded(psiFile, pluginConfig);
	}
	
	private boolean hasDocument(final PsiFile psiFile)
	{
		return PsiDocumentManager.getInstance(psiFile.getProject()).getDocument(psiFile) != null;
	}
	
	private boolean isValidFileType(
		final PsiFile psiFile,
		final PluginConfiguration pluginConfig)
	{
		return !pluginConfig.scanScope().includeOnlySupportedSources()
			|| ApplicationManager.getApplication()
			.getService(LanguageVersionResolverService.class)
			.isFileSupportedByAnyResolver(psiFile);
	}
	
	private boolean isScannableIfTest(
		final PsiFile psiFile,
		final PluginConfiguration pluginConfig)
	{
		return pluginConfig.scanScope().includeTestClasses()
			|| !this.isInTestSource(psiFile);
	}
	
	private boolean isGenerated(final PsiFile psiFile)
	{
		return JavaProjectRootsUtil.isInGeneratedCode(psiFile.getVirtualFile(), psiFile.getProject());
	}
	
	private boolean isInSource(@NotNull final PsiFile psiFile)
	{
		return psiFile.getVirtualFile() != null
			&& ProjectFileIndex.getInstance(psiFile.getProject()).isInSourceContent(psiFile.getVirtualFile());
	}
	
	private boolean isInTestSource(final PsiElement element)
	{
		final VirtualFile elementFile = element.getContainingFile().getVirtualFile();
		if(elementFile == null)
		{
			return false;
		}
		
		final Module module = ModuleUtilCore.findModuleForPsiElement(element);
		if(module == null)
		{
			return false;
		}
		
		final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
		return moduleRootManager != null
			&& moduleRootManager.getFileIndex().isInTestSourceContent(elementFile);
	}
	
	private boolean modulesMatch(
		final PsiFile psiFile,
		final Optional<Module> optModule)
	{
		return optModule
			.map(module -> {
				final Module elementModule = ModuleUtilCore.findModuleForPsiElement(psiFile);
				return elementModule != null && elementModule.equals(module);
			})
			.orElse(true);
	}
	
	private boolean isExcluded(
		final PsiFile psiFile,
		final PluginConfiguration pluginConfig)
	{
		if(pluginConfig.projectRelativeFileExclusions().isEmpty())
		{
			return false;
		}
		
		final VirtualFile psiFileVF = psiFile.getVirtualFile();
		final String path = psiFileVF.getCanonicalPath();
		if(path == null)
		{
			return false;
		}
		
		final Map<String, Boolean> fileExclusionCache = this.pluginConfigFileExclusionCache.computeIfAbsent(
			pluginConfig,
			ignored -> new ConcurrentReferenceHashMap<>(256));
		return fileExclusionCache.computeIfAbsent(
			path,
			ignored -> {
				// Try to relativize the path so that no matching outside the project occurs
				final String determinedPath = Optional.ofNullable(ProjectUtil.guessProjectDir(psiFile.getProject()))
					.map(projectVF -> VfsUtilCore.getRelativePath(psiFileVF, projectVF))
					.orElse(path);
				
				return pluginConfig.projectRelativeFileExclusions()
					.stream()
					.anyMatch(c -> c.pattern().matcher(determinedPath).matches());
			}
		);
	}
	
	@Override
	public void dispose()
	{
		this.pluginConfigFileExclusionCache.clear();
	}
}
