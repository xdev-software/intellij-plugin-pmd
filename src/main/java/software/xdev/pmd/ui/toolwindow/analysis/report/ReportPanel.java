package software.xdev.pmd.ui.toolwindow.analysis.report;

import com.intellij.openapi.project.Project;

import software.xdev.pmd.currentfile.CombinedPMDAnalysisResult;
import software.xdev.pmd.ui.toolwindow.analysis.AnalysisPanel;


public class ReportPanel extends AnalysisPanel
{
	public ReportPanel(final Project project, final CombinedPMDAnalysisResult result)
	{
		super(project);
		this.updateTree(result);
	}
}
