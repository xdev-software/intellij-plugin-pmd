package software.xdev.pmd.ui.toolwindow.node;

import com.intellij.psi.PsiFile;

import net.sourceforge.pmd.reporting.RuleViolation;
import software.xdev.pmd.ui.toolwindow.node.has.HasViolationCount;


public class ViolationNode extends BaseViolationNode implements HasViolationCount
{
	public ViolationNode(final RuleViolation violation, final PsiFile psiFile)
	{
		super(violation, psiFile);
	}
	
	@Override
	public int violationCount()
	{
		return 1;
	}
}
