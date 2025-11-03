package software.xdev.pmd.analysis;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.intellij.psi.PsiFile;

import net.sourceforge.pmd.lang.document.FileId;
import net.sourceforge.pmd.reporting.Report;


public record PMDAnalysisResult(
	@Nullable Report report,
	Map<FileId, PsiFile> fileIdPsiFiles
)
{
	public static PMDAnalysisResult empty()
	{
		return new PMDAnalysisResult(null, new HashMap<>());
	}
}
