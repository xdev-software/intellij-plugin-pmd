package software.xdev.pmd.langversion;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.psi.PsiFile;

import net.sourceforge.pmd.lang.Language;
import software.xdev.pmd.util.ep.HasOrder;


public interface LanguageResolver extends HasOrder
{
	boolean isFileSupported(@NotNull PsiFile file);
	
	@Nullable
	Language resolveLanguage(@NotNull PsiFile file);
}
