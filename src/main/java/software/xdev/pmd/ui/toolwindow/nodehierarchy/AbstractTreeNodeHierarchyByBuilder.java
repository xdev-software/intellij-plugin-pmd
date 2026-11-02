package software.xdev.pmd.ui.toolwindow.nodehierarchy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.intellij.psi.PsiFile;

import software.xdev.pmd.currentfile.CombinedPMDAnalysisResult;
import software.xdev.pmd.ui.toolwindow.node.ErrorInFileNode;
import software.xdev.pmd.ui.toolwindow.node.ErrorNode;
import software.xdev.pmd.ui.toolwindow.node.FileOverviewNode;
import software.xdev.pmd.ui.toolwindow.node.other.NodeErrorAdapter;


public abstract class AbstractTreeNodeHierarchyByBuilder extends BaseTreeNodeHierarchyBuilder
{
	protected final List<ErrorNode> generalErrors = new ArrayList<>();
	protected final Map<FileOverviewNode, Set<ErrorInFileNode>> errorsInFiles = new HashMap<>();
	
	protected AbstractTreeNodeHierarchyByBuilder(final CombinedPMDAnalysisResult result)
	{
		super(result);
	}
	
	protected void computeGeneralErrors()
	{
		this.result.configErrors().stream()
			.map(ce -> new ErrorNode(NodeErrorAdapter.fromString(ce.rule().getName() + ": " + ce.issue())))
			.forEach(this.generalErrors::add);
	}
	
	protected void computeErrors(final Function<PsiFile, FileOverviewNode> createFileOverviewNode)
	{
		this.result.errors().forEach(pe -> {
			final NodeErrorAdapter nodeErrorAdapter = NodeErrorAdapter.fromThrowable(pe.getError());
			Optional.ofNullable(pe.getFileId())
				.map(this.fileIdPsiFiles::get)
				.ifPresentOrElse(
					file -> this.errorsInFiles.computeIfAbsent(
							createFileOverviewNode.apply(file),
							ignored -> new LinkedHashSet<>())
						.add(new ErrorInFileNode(nodeErrorAdapter, file)),
					() -> this.generalErrors.add(new ErrorNode(nodeErrorAdapter))
				);
		});
	}
}
