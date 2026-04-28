package software.xdev.pmd.langversion;

import org.jetbrains.kotlin.idea.base.projectStructure.LanguageVersionSettingsProviderUtils;
import org.jetbrains.kotlin.psi.KtFile;


public class KotlinLanguageAndVersionResolver extends CombinedLanguageAndVersionResolver<KtFile>
{
	public KotlinLanguageAndVersionResolver()
	{
		super("kotlin", KtFile.class);
	}
	
	@Override
	protected String resolveLangVersionForFile(final KtFile file)
	{
		return LanguageVersionSettingsProviderUtils.getLanguageVersionSettings(file)
			.getLanguageVersion()
			.getVersionString();
	}
}
