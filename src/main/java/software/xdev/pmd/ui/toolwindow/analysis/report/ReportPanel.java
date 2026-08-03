package software.xdev.pmd.ui.toolwindow.analysis.report;

import org.jetbrains.annotations.NotNull;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import software.xdev.pmd.action.analysis.ActionFilesAnalyzer;
import software.xdev.pmd.action.analysis.ReplayableAnalysisInfo;
import software.xdev.pmd.currentfile.CombinedPMDAnalysisResult;
import software.xdev.pmd.ui.toolwindow.analysis.AnalysisPanel;
import software.xdev.pmd.ui.toolwindow.nodehierarchy.TreeNodeHierarchyFactories;


public class ReportPanel extends AnalysisPanel
{
	public ReportPanel(
		final Project project,
		final CombinedPMDAnalysisResult result,
		final ReplayableAnalysisInfo replayableAnalysisInfo)
	{
		super(project, TreeNodeHierarchyFactories.BY_RULE);
		
		if(replayableAnalysisInfo != null)
		{
			this.toolbarActionGroup.add(new Separator());
			this.toolbarActionGroup.add(new ReRunAction(replayableAnalysisInfo));
		}
		
		this.updateTree(result);
	}
	
	static class ReRunAction extends AnAction
	{
		private final ReplayableAnalysisInfo replayableAnalysisInfo;
		
		ReRunAction(final ReplayableAnalysisInfo replayableAnalysisInfo)
		{
			super("Rerun", "Rerun analysis", AllIcons.Actions.Rerun);
			this.replayableAnalysisInfo = replayableAnalysisInfo;
		}
		
		@Override
		public void actionPerformed(@NotNull final AnActionEvent e)
		{
			ApplicationManager.getApplication()
				.getService(ActionFilesAnalyzer.class)
				.analyze(this.replayableAnalysisInfo);
		}
	}
}
