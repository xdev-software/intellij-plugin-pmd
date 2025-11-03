package software.xdev.pmd.langversion;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiFile;

import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageVersion;


public class LanguageVersionResolverService
{
	private final ExtensionPointName<LanguageResolver> epLang =
		ExtensionPointName.create("software.xdev.pmd.languageResolver");
	
	private final ExtensionPointName<LanguageVersionResolver> epVersion =
		ExtensionPointName.create("software.xdev.pmd.languageVersionResolver");
	
	private List<LanguageResolver> lastSeenLangExtensions;
	private List<LanguageResolver> cachedOrderedLangResolvers;
	
	private List<LanguageVersionResolver> lastSeenVersionExtensions;
	private Map<Language, List<LanguageVersionResolver>> cachedOrderedVersionResolvers;
	
	private List<LanguageResolver> orderedLangResolvers()
	{
		final List<LanguageResolver> extensions = this.epLang.getExtensionList();
		if(this.cachedOrderedLangResolvers == null || extensions != this.lastSeenLangExtensions)
		{
			this.cachedOrderedLangResolvers = extensions
				.stream()
				.sorted(Comparator.comparingInt(LanguageResolver::order))
				.toList();
			this.lastSeenLangExtensions = extensions;
		}
		return this.cachedOrderedLangResolvers;
	}
	
	private Map<Language, List<LanguageVersionResolver>> orderedVersionResolvers()
	{
		final List<LanguageVersionResolver> extensions = this.epVersion.getExtensionList();
		if(this.cachedOrderedVersionResolvers == null || extensions != this.lastSeenVersionExtensions)
		{
			this.cachedOrderedVersionResolvers = extensions
				.stream()
				.flatMap(r -> r.supportedLanguages().stream()
					.map(l -> Map.entry(l, r)))
				.collect(Collectors.groupingBy(
					Map.Entry::getKey,
					Collectors.mapping(
						Map.Entry::getValue,
						Collectors.collectingAndThen(
							Collectors.toList(),
							l -> l.stream()
								.sorted(Comparator.comparingInt(LanguageVersionResolver::order))
								.toList()
						))));
			this.lastSeenVersionExtensions = extensions;
		}
		return this.cachedOrderedVersionResolvers;
	}
	
	public Optional<Language> resolveLanguage(@NotNull final PsiFile file)
	{
		return this.orderedLangResolvers()
			.stream()
			.map(r -> r.resolveLanguage(file))
			.filter(Objects::nonNull)
			.findFirst();
	}
	
	public Optional<LanguageVersion> resolveVersion(@NotNull final Language language, @NotNull final PsiFile file)
	{
		final List<LanguageVersionResolver> languageVersionResolvers = this.orderedVersionResolvers().get(language);
		if(languageVersionResolvers == null)
		{
			return Optional.empty();
		}
		
		return ApplicationManager.getApplication()
			.runReadAction((Computable<Optional<LanguageVersion>>)() -> languageVersionResolvers
				.stream()
				.map(r -> r.resolveVersion(language, file))
				.filter(Objects::nonNull)
				.findFirst());
	}
	
	public Set<String> supportedLanguageIds()
	{
		return this.orderedVersionResolvers().keySet()
			.stream()
			.map(Language::getId)
			.sorted()
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}
	
	public boolean isFileSupportedByAnyResolver(final PsiFile file)
	{
		return this.orderedLangResolvers().stream().anyMatch(r -> r.isFileSupported(file));
	}
}
