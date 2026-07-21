package software.xdev.pmd.action.analysispanel;

import java.awt.Component;
import java.awt.KeyboardFocusManager;

import javax.swing.SwingUtilities;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;

import software.xdev.pmd.ui.toolwindow.PMDToolWindowFactory;
import software.xdev.pmd.ui.toolwindow.analysis.AnalysisPanel;


public abstract class AnalysisPanelKeyboardAction extends AnAction
{
	@Override
	public void actionPerformed(@NotNull final AnActionEvent e)
	{
		final Project project = getEventProject(e);
		if(project == null)
		{
			return;
		}
		
		final ToolWindow toolWindow = ToolWindowManager.getInstance(project)
			.getToolWindow(PMDToolWindowFactory.TOOL_WINDOW_ID);
		if(toolWindow == null || !toolWindow.isVisible())
		{
			return;
		}
		
		final Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		if(focusOwner == null || !SwingUtilities.isDescendingFrom(focusOwner, toolWindow.getComponent()))
		{
			return;
		}
		
		final Content selectedContent = toolWindow.getContentManager().getSelectedContent();
		if(selectedContent == null
			|| !(selectedContent.getComponent() instanceof final AnalysisPanel analysisPanel))
		{
			return;
		}
		
		this.exec(analysisPanel);
	}
	
	protected abstract void exec(AnalysisPanel analysisPanel);
	
	@Override
	public @NotNull ActionUpdateThread getActionUpdateThread()
	{
		return ActionUpdateThread.EDT;
	}
}
