package software.xdev.pmd.ui.toolwindow.node;

import net.sourceforge.pmd.lang.rule.Rule;
import software.xdev.pmd.ui.toolwindow.node.has.HasViolationCount;


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
	public int violationCount()
	{
		return this.count;
	}
}
