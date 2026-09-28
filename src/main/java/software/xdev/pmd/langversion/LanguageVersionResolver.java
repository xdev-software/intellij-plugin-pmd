package software.xdev.pmd.langversion;

import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.psi.PsiFile;

import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageVersion;
import software.xdev.pmd.util.ep.HasOrder;


public interface LanguageVersionResolver extends HasOrder
{
	@NotNull
	Set<Language> supportedLanguages();
	
	@Nullable
	LanguageVersion resolveVersion(@NotNull Language language, @NotNull PsiFile file);
}
