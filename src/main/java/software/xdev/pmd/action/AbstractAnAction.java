package software.xdev.pmd.action;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;


public abstract class AbstractAnAction extends AnAction
{
	@Override
	public void update(@NotNull final AnActionEvent ev)
	{
		final Project project = ev.getProject();
		if(project == null || !project.isInitialized() || project.isDisposed())
		{
			ev.getPresentation().setEnabledAndVisible(false);
			return;
		}
		final boolean visible = this.isVisible(ev, project);
		ev.getPresentation().setVisible(visible);
		if(!visible)
		{
			ev.getPresentation().setEnabled(false);
			return;
		}
		
		ev.getPresentation().setEnabled(this.isEnabled(ev, project));
	}
	
	protected boolean isVisible(final AnActionEvent ev, final Project project)
	{
		return true;
	}
	
	protected boolean isEnabled(final AnActionEvent ev, final Project project)
	{
		return true;
	}
	
	@Override
	public @NotNull ActionUpdateThread getActionUpdateThread()
	{
		return ActionUpdateThread.BGT;
	}
}
