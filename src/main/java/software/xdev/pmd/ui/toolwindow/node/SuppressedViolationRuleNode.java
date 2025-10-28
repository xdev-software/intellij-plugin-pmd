package software.xdev.pmd.ui.toolwindow.node;

import org.jetbrains.annotations.NotNull;

import com.intellij.icons.AllIcons;

import net.sourceforge.pmd.lang.rule.Rule;
import software.xdev.pmd.ui.toolwindow.node.has.HasSuppressedViolationCount;
import software.xdev.pmd.ui.toolwindow.node.render.NodeCellRenderer;


public class SuppressedViolationRuleNode extends BaseRuleNode implements HasSuppressedViolationCount
{
	public SuppressedViolationRuleNode(final Rule rule)
	{
		super(rule);
	}
	
	@Override
	protected int childrenSum()
	{
		return this.childrenSum(HasSuppressedViolationCount.class, HasSuppressedViolationCount::suppressedCount);
	}
	
	@Override
	public void render(@NotNull final NodeCellRenderer renderer)
	{
		renderer.setIcon(AllIcons.General.Information);
		super.render(renderer);
	}
	
	@Override
	public int suppressedCount()
	{
		return this.count;
	}
}
