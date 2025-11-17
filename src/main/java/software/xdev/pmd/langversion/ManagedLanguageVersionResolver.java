package software.xdev.pmd.langversion;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;

import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageVersion;


public class ManagedLanguageVersionResolver
{
	private final LanguageVersionResolverService resolverService =
		ApplicationManager.getApplication().getService(LanguageVersionResolverService.class);
	
	public Optional<LanguageVersion> resolveLanguage(final PsiFile file)
	{
		return this.resolverService.resolveLanguage(file)
			.map(lang -> this.resolveWithLang(lang, file));
	}
	
	@NotNull
	public LanguageVersion resolveWithLang(@NotNull final Language language, @NotNull final PsiFile file)
	{
		return this.resolverService.resolveVersion(language, file)
			// Fallback to latest version
			.orElseGet(language::getLatestVersion);
	}
}
