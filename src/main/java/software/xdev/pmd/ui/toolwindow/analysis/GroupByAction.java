package software.xdev.pmd.ui.toolwindow.analysis;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareToggleAction;

import software.xdev.pmd.ui.toolwindow.nodehierarchy.TreeNodeHierarchyBuilderFactory;


public class GroupByAction extends DumbAwareToggleAction
{
	private final GroupByActionTarget target;
	private final TreeNodeHierarchyBuilderFactory builderFactory;
	
	public GroupByAction(
		final GroupByActionTarget target,
		final TreeNodeHierarchyBuilderFactory builderFactory)
	{
		super(
			"Group by " + builderFactory.name(),
			"Group results by " + builderFactory.name(),
			builderFactory.icon());
		this.target = target;
		this.builderFactory = builderFactory;
	}
	
	@Override
	public void update(@NotNull final AnActionEvent e)
	{
		e.getPresentation().setEnabled(this.target.isGroupByActionAvailable());
		super.update(e);
	}
	
	@Override
	public boolean isSelected(@NotNull final AnActionEvent e)
	{
		return this.target.getCurrentHierarchyBuilderFactory() == this.builderFactory;
	}
	
	@Override
	public void setSelected(@NotNull final AnActionEvent e, final boolean state)
	{
		if(!state)
		{
			return;
		}
		
		this.target.setCurrentHierarchyBuilderFactoryAndUpdate(this.builderFactory);
	}
	
	@Override
	public @NotNull ActionUpdateThread getActionUpdateThread()
	{
		return ActionUpdateThread.BGT;
	}
}
