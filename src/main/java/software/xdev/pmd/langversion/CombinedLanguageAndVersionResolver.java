package software.xdev.pmd.langversion;

import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.psi.PsiFile;

import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;


public abstract class CombinedLanguageAndVersionResolver<F extends PsiFile>
	implements LanguageVersionResolver, LanguageResolver
{
	private final String languageId;
	private final Class<F> fileClazz;
	
	private Language language;
	
	protected CombinedLanguageAndVersionResolver(final String languageId, final Class<F> fileClazz)
	{
		this.languageId = languageId;
		this.fileClazz = fileClazz;
	}
	
	protected Language language()
	{
		if(this.language == null)
		{
			this.language = LanguageRegistry.PMD.getLanguageById(this.languageId);
		}
		return this.language;
	}
	
	@NotNull
	@Override
	public Set<Language> supportedLanguages()
	{
		return Set.of(this.language());
	}
	
	@Override
	public boolean isFileSupported(@NotNull final PsiFile file)
	{
		return this.fileClazz.isInstance(file);
	}
	
	@Override
	public @Nullable Language resolveLanguage(@NotNull final PsiFile file)
	{
		if(!this.isFileSupported(file))
		{
			return null;
		}
		
		return this.language();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public @Nullable LanguageVersion resolveVersion(@NotNull final Language language, @NotNull final PsiFile file)
	{
		if(!this.isFileSupported(file))
		{
			return null;
		}
		
		final String version = this.resolveLangVersionForFile((F)file);
		return version != null ? language.getVersion(version) : null;
	}
	
	protected abstract String resolveLangVersionForFile(F file);
	
	@Override
	public int order()
	{
		return LanguageVersionResolver.super.order();
	}
}
