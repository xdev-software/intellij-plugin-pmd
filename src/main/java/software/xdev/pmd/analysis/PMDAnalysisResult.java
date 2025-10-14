package software.xdev.pmd.analysis;

import com.intellij.psi.PsiFile;
import com.intellij.util.containers.BidirectionalMap;

import net.sourceforge.pmd.lang.document.FileId;
import net.sourceforge.pmd.reporting.Report;


public record PMDAnalysisResult(
	Report report,
	BidirectionalMap<FileId, PsiFile> fileIdPsiFiles
)
{
}
