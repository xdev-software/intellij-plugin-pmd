package software.xdev.pmd.ui.toolwindow.node;

import org.jetbrains.annotations.NotNull;

import net.sourceforge.pmd.lang.rule.Rule;
import software.xdev.pmd.ui.toolwindow.node.has.HasViolationCount;
import software.xdev.pmd.ui.toolwindow.node.other.RulePriorityIcons;
import software.xdev.pmd.ui.toolwindow.node.render.NodeCellRenderer;


public class ViolationRuleNode extends BaseRuleNode implements HasViolationCount
{
	public ViolationRuleNode(final Rule rule)
	{
		super(rule);
	}
	
	@Override
	protected int childrenSum()
	{
		return this.childrenSum(HasViolationCount.class, HasViolationCount::violationCount);
	}
	
	@Override
	public void render(@NotNull final NodeCellRenderer renderer)
	{
		renderer.setIcon(RulePriorityIcons.get(this.rule.getPriority()));
		super.render(renderer);
	}
	
	@Override
	public int violationCount()
	{
		return this.count;
	}
}
