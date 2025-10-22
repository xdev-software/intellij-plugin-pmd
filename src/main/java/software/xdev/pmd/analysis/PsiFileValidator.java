package software.xdev.pmd.analysis;

import java.util.Collection;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.roots.JavaProjectRootsUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import software.xdev.pmd.config.PluginConfiguration;
import software.xdev.pmd.langversion.LanguageVersionResolverService;
import software.xdev.pmd.model.config.ConfigurationLocation;
import software.xdev.pmd.model.scope.NamedScopeHelper;
import software.xdev.pmd.model.scope.ScanScope;


final class PsiFileValidator
{
	private PsiFileValidator()
	{
	}
	
	public static boolean isScannable(
		@Nullable final PsiFile psiFile,
		@NotNull final Optional<Module> optModule,
		@NotNull final PluginConfiguration pluginConfig)
	{
		return psiFile != null
			&& psiFile.isValid()
			&& psiFile.isPhysical()
			&& hasDocument(psiFile)
			&& isInSource(psiFile, pluginConfig)
			&& isValidFileType(psiFile, pluginConfig)
			&& isScannableIfTest(psiFile, pluginConfig)
			&& modulesMatch(psiFile, optModule)
			&& !isGenerated(psiFile);
	}
	
	private static boolean hasDocument(final PsiFile psiFile)
	{
		return PsiDocumentManager.getInstance(psiFile.getProject()).getDocument(psiFile) != null;
	}
	
	private static boolean isValidFileType(
		final PsiFile psiFile,
		final PluginConfiguration pluginConfig)
	{
		return !pluginConfig.getScanScope().includeOnlySupportedSources()
			|| ApplicationManager.getApplication()
			.getService(LanguageVersionResolverService.class)
			.isFileSupportedByAnyResolve(psiFile);
	}
	
	private static boolean isScannableIfTest(
		final PsiFile psiFile,
		final PluginConfiguration pluginConfig)
	{
		return pluginConfig.getScanScope().includeTestClasses()
			|| !isTestClass(psiFile);
	}
	
	private static boolean isGenerated(final PsiFile psiFile)
	{
		return JavaProjectRootsUtil.isInGeneratedCode(psiFile.getVirtualFile(), psiFile.getProject());
	}
	
	private static boolean isInSource(
		@NotNull final PsiFile psiFile,
		@NotNull final PluginConfiguration pluginConfig)
	{
		final boolean shouldBeScanned = pluginConfig.getScanScope() == ScanScope.EVERYTHING
			|| psiFile.getVirtualFile() != null
			&& ProjectFileIndex.getInstance(psiFile.getProject()).isInSourceContent(psiFile.getVirtualFile());
		return shouldBeScanned && isInNamedScopeIfPresent(
			psiFile,
			pluginConfig.getActiveLocations());
	}
	
	/**
	 * Returns true, if the given psiFile is contained in any named scope of the given pluginConfig. If no
	 * NamedScope is
	 * provided, true will be returned.
	 */
	private static boolean isInNamedScopeIfPresent(
		@NotNull final PsiFile psiFile,
		final Collection<ConfigurationLocation> activeLocations)
	{
		return activeLocations.stream()
			.map(ConfigurationLocation::getNamedScope)
			.flatMap(Optional::stream)
			.anyMatch(scope -> NamedScopeHelper.isFileInScope(psiFile, scope));
	}
	
	private static boolean isTestClass(final PsiElement element)
	{
		final VirtualFile elementFile = element.getContainingFile().getVirtualFile();
		if(elementFile == null)
		{
			return false;
		}
		
		final Module module = ModuleUtil.findModuleForPsiElement(element);
		if(module == null)
		{
			return false;
		}
		
		final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
		return moduleRootManager != null
			&& moduleRootManager.getFileIndex().isInTestSourceContent(elementFile);
	}
	
	private static boolean modulesMatch(
		final PsiFile psiFile,
		final Optional<Module> optModule)
	{
		if(optModule.isEmpty())
		{
			return true;
		}
		final Module elementModule = ModuleUtil.findModuleForPsiElement(psiFile);
		return elementModule != null && elementModule.equals(optModule.get());
	}
}
