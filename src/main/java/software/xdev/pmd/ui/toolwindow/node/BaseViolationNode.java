package software.xdev.pmd.ui.toolwindow.node;

import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.google.common.base.Suppliers;
import com.intellij.psi.PsiFile;

import net.sourceforge.pmd.reporting.RuleViolation;
import software.xdev.pmd.ui.toolwindow.node.has.HasPositionInFile;
import software.xdev.pmd.ui.toolwindow.node.other.FilePosition;
import software.xdev.pmd.ui.toolwindow.node.render.NodeCellRenderer;


public abstract class BaseViolationNode extends BaseNode implements HasPositionInFile
{
	protected final RuleViolation violation;
	protected final Supplier<FilePosition> filePositionSupplier;
	
	protected BaseViolationNode(final RuleViolation violation, final PsiFile psiFile)
	{
		this.violation = violation;
		this.filePositionSupplier = Suppliers.memoize(() -> new FilePosition(psiFile, violation.getLocation()));
	}
	
	@Override
	public void render(@NotNull final NodeCellRenderer renderer)
	{
		renderer.append("[" + this.violation.getBeginLine()
			+ ":"
			+ this.violation.getBeginColumn()
			+ "] "
			+ this.violation.getDescription());
	}
	
	@Override
	public Supplier<FilePosition> filePositionSupplier()
	{
		return this.filePositionSupplier;
	}
}
