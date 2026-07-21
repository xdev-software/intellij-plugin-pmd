package software.xdev.pmd.action.analysispanel;

import software.xdev.pmd.ui.toolwindow.analysis.AnalysisPanel;


public class AnalysisPanelJumpToNextAction extends AnalysisPanelKeyboardAction
{
	@Override
	protected void exec(final AnalysisPanel analysisPanel)
	{
		analysisPanel.navigateToSiblingResult(true);
	}
}
