package software.xdev.pmd.ui.toolwindow.node;

import net.sourceforge.pmd.lang.rule.Rule;
import software.xdev.pmd.ui.toolwindow.node.has.HasSuppressedViolationCount;


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
	public int suppressedCount()
	{
		return this.count;
	}
}
