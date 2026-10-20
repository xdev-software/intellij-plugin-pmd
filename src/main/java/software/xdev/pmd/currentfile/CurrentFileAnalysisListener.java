package software.xdev.pmd.currentfile;

import org.jetbrains.annotations.Nullable;

import com.intellij.psi.PsiFile;


public interface CurrentFileAnalysisListener
{
	/**
	 * Invoked when the current file was changed or analyzed
	 */
	void onChange(@Nullable PsiFile psiFile, CombinedPMDAnalysisResult results);
}
