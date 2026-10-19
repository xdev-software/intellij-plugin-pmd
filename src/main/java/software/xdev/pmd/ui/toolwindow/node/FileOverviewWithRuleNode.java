package software.xdev.pmd.ui.toolwindow.node;

import com.intellij.psi.PsiFile;

import net.sourceforge.pmd.lang.rule.Rule;
import software.xdev.pmd.ui.toolwindow.node.has.HasRule;


public class FileOverviewWithRuleNode extends FileOverviewNode implements HasRule
{
	private final Rule rule;
	
	public FileOverviewWithRuleNode(final PsiFile psiFile, final Rule rule)
	{
		super(psiFile);
		this.rule = rule;
	}
	
	@Override
	public Rule getRule()
	{
		return this.rule;
	}
}
