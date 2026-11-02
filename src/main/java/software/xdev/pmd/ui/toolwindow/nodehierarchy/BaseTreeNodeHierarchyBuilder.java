package software.xdev.pmd.ui.toolwindow.nodehierarchy;

import java.util.List;
import java.util.Map;

import com.intellij.psi.PsiFile;

import net.sourceforge.pmd.lang.document.FileId;
import software.xdev.pmd.currentfile.CombinedPMDAnalysisResult;
import software.xdev.pmd.ui.toolwindow.node.BaseNode;


public abstract class BaseTreeNodeHierarchyBuilder
{
	protected final CombinedPMDAnalysisResult result;
	protected final Map<FileId, PsiFile> fileIdPsiFiles;
	
	protected BaseTreeNodeHierarchyBuilder(final CombinedPMDAnalysisResult result)
	{
		this.result = result;
		this.fileIdPsiFiles = result.fileIdPsiFiles();
	}
	
	public abstract List<BaseNode> build();
}
