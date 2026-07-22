package software.xdev.pmd.action.analysispanel;

import software.xdev.pmd.ui.toolwindow.analysis.AnalysisPanel;


public class JumpToNextGlobalAnalysisPanelAction extends AnalysisPanelKeyboardAction
{
	@Override
	protected void exec(final AnalysisPanel analysisPanel)
	{
		analysisPanel.navigateToSiblingResultAndNavigate(true);
	}
}
