package software.xdev.pmd.ui.toolwindow.node;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.intellij.icons.AllIcons;

import software.xdev.pmd.ui.toolwindow.node.has.HasErrorCount;
import software.xdev.pmd.ui.toolwindow.node.render.NodeCellRenderer;


public class ErrorSummaryNode extends BaseNode implements HasErrorCount
{
	private int errorCount;
	
	public ErrorSummaryNode(final Collection<? extends ErrorNode> errorNodesToAdd)
	{
		errorNodesToAdd.forEach(this::add);
	}
	
	@Override
	public void update()
	{
		this.errorCount = this.childrenSum(HasErrorCount.class, HasErrorCount::errorCount);
	}
	
	@Override
	public void render(@NotNull final NodeCellRenderer renderer)
	{
		renderer.setIcon(AllIcons.General.Error);
		renderer.append("Errors (" + this.errorCount + ")");
	}
	
	@Override
	public int errorCount()
	{
		return this.errorCount;
	}
}
