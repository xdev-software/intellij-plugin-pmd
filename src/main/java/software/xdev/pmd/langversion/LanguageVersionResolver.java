package software.xdev.pmd.langversion;

import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.psi.PsiFile;

import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageVersion;


public interface LanguageVersionResolver
{
	default int order()
	{
		return 1000;
	}
	
	@NotNull
	Set<Language> supportedLanguages();
	
	@Nullable
	LanguageVersion resolveVersion(@NotNull Language language, @NotNull PsiFile file);
}
