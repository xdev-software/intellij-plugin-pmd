package software.xdev.pmd.ui.toolwindow.node;

import com.intellij.psi.PsiFile;

import net.sourceforge.pmd.reporting.RuleViolation;
import software.xdev.pmd.ui.toolwindow.node.has.HasSuppressedViolationCount;


public class SuppressedViolationNode extends BaseViolationNode implements HasSuppressedViolationCount
{
	public SuppressedViolationNode(final RuleViolation violation, final PsiFile psiFile)
	{
		super(violation, psiFile);
	}
	
	@Override
	public int suppressedCount()
	{
		return 1;
	}
}
