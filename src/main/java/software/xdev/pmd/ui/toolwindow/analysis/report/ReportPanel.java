package software.xdev.pmd.ui.toolwindow.analysis.report;

import com.intellij.openapi.project.Project;

import software.xdev.pmd.currentfile.CombinedPMDAnalysisResult;
import software.xdev.pmd.ui.toolwindow.analysis.AnalysisPanel;
import software.xdev.pmd.ui.toolwindow.nodehierarchy.TreeNodeHierarchyFactories;


public class ReportPanel extends AnalysisPanel
{
	public ReportPanel(final Project project, final CombinedPMDAnalysisResult result)
	{
		super(project, TreeNodeHierarchyFactories.BY_RULE);
		this.updateTree(result);
	}
}
