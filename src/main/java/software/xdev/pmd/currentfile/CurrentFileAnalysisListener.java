package software.xdev.pmd.currentfile;

import java.util.List;

import com.intellij.psi.PsiFile;

import software.xdev.pmd.analysis.PMDAnalysisResult;


public interface CurrentFileAnalysisListener
{
	void onAnalyzed(PsiFile psiFile, List<PMDAnalysisResult> results);
}
