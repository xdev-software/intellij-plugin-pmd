package software.xdev.pmd.langversion;

import com.intellij.psi.PsiJavaFile;


public class JavaLanguageAndVersionResolver extends CombinedLanguageAndVersionResolver<PsiJavaFile>
{
	public JavaLanguageAndVersionResolver()
	{
		super("java", PsiJavaFile.class);
	}
	
	@Override
	protected String resolveLangVersionForFile(final PsiJavaFile file)
	{
		return file.getLanguageLevel().toJavaVersion().toString();
	}
}
