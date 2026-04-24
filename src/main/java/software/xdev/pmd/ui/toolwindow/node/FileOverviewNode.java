package software.xdev.pmd.ui.toolwindow.node;

import java.util.function.Supplier;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;

import com.google.common.base.Suppliers;
import com.intellij.openapi.application.ReadAction;
import com.intellij.psi.PsiFile;

import software.xdev.pmd.ui.toolwindow.node.has.HasErrorCount;
import software.xdev.pmd.ui.toolwindow.node.has.HasPositionInFile;
import software.xdev.pmd.ui.toolwindow.node.has.HasSuppressedViolationCount;
import software.xdev.pmd.ui.toolwindow.node.has.HasViolationCount;
import software.xdev.pmd.ui.toolwindow.node.other.FilePosition;
import software.xdev.pmd.ui.toolwindow.node.render.NodeCellRenderer;


public class FileOverviewNode extends BaseHasViolationSuppressedErrorNode
	implements HasPositionInFile, HasViolationCount, HasSuppressedViolationCount, HasErrorCount
{
	private final PsiFile psiFile;
	private final Supplier<FilePosition> filePositionSupplier;
	private final Icon icon;
	
	public FileOverviewNode(final PsiFile psiFile)
	{
		this.psiFile = psiFile;
		// Icon requires read access so let's get it here
		this.icon = ReadAction.compute(() -> psiFile.getIcon(0));
		
		this.filePositionSupplier = Suppliers.memoize(() -> new FilePosition(psiFile));
	}
	
	@Override
	public void render(@NotNull final NodeCellRenderer renderer)
	{
		renderer.setIcon(this.icon);
		renderer.append(this.psiFile.getName()
			+ " (" + this.violationsSuppressedErrorToString() + ")");
	}
	
	@Override
	public Supplier<FilePosition> filePositionSupplier()
	{
		return this.filePositionSupplier;
	}
	
	@Override
	public int violationCount()
	{
		return this.violationCount;
	}
	
	@Override
	public int suppressedCount()
	{
		return this.suppressedCount;
	}
	
	@Override
	public int errorCount()
	{
		return this.errorCount;
	}
}
