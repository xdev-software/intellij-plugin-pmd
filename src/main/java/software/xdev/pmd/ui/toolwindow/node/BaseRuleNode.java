package software.xdev.pmd.ui.toolwindow.node;

import org.jetbrains.annotations.NotNull;

import net.sourceforge.pmd.lang.rule.Rule;
import software.xdev.pmd.ui.toolwindow.node.has.HasRule;
import software.xdev.pmd.ui.toolwindow.node.render.NodeCellRenderer;


public abstract class BaseRuleNode extends BaseNode implements HasRule
{
	protected final Rule rule;
	protected int count;
	
	protected BaseRuleNode(final Rule rule)
	{
		this.rule = rule;
	}
	
	@Override
	public void update()
	{
		this.count = this.childrenSum();
	}
	
	protected abstract int childrenSum();
	
	@Override
	public void render(@NotNull final NodeCellRenderer renderer)
	{
		renderer.append(this.rule.getName() + " (" + this.count + "x)");
	}
	
	@Override
	public Rule getRule()
	{
		return this.rule;
	}
}
