package software.xdev.pmd.langversion;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.psi.PsiFile;

import net.sourceforge.pmd.lang.Language;


public interface LanguageResolver
{
	default int order()
	{
		return 1000;
	}
	
	boolean isFileSupported(@NotNull PsiFile file);
	
	@Nullable
	Language resolveLanguage(@NotNull PsiFile file);
}
