package software.xdev.pmd.ui.toolwindow.nodehierarchy;

import java.util.function.Function;

import javax.swing.Icon;

import software.xdev.pmd.currentfile.CombinedPMDAnalysisResult;


public record TreeNodeHierarchyBuilderFactory(
	Function<CombinedPMDAnalysisResult, BaseTreeNodeHierarchyBuilder> createBuilder,
	String name,
	Icon icon
)
{
}
