package software.xdev.pmd.ui.toolwindow.node;

import org.jetbrains.annotations.NotNull;

import com.intellij.icons.AllIcons;

import software.xdev.pmd.ui.toolwindow.node.has.HasSuppressedViolationCount;
import software.xdev.pmd.ui.toolwindow.node.render.NodeCellRenderer;


public class SuppressedSummaryNode extends BaseNode implements HasSuppressedViolationCount
{
	private int suppressedCount;
	
	@Override
	public void update()
	{
		this.suppressedCount =
			this.childrenSum(HasSuppressedViolationCount.class, HasSuppressedViolationCount::suppressedCount);
	}
	
	@Override
	public void render(@NotNull final NodeCellRenderer renderer)
	{
		renderer.setIcon(AllIcons.General.Information);
		renderer.append("Suppressed (" + this.suppressedCount + " violations)");
	}
	
	@Override
	public int suppressedCount()
	{
		return this.suppressedCount;
	}
}
