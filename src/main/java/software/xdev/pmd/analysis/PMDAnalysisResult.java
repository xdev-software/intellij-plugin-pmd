package software.xdev.pmd.analysis;

import org.jetbrains.annotations.Nullable;

import com.intellij.psi.PsiFile;
import com.intellij.util.containers.BidirectionalMap;

import net.sourceforge.pmd.lang.document.FileId;
import net.sourceforge.pmd.reporting.Report;


public record PMDAnalysisResult(
	@Nullable Report report,
	BidirectionalMap<FileId, PsiFile> fileIdPsiFiles
)
{
	public static PMDAnalysisResult empty()
	{
		return new PMDAnalysisResult(null, new BidirectionalMap<>());
	}
}
