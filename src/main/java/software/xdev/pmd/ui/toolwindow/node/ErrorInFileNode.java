package software.xdev.pmd.ui.toolwindow.node;

import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.intellij.psi.PsiFile;

import software.xdev.pmd.ui.toolwindow.node.has.HasPositionInFile;
import software.xdev.pmd.ui.toolwindow.node.other.FilePosition;
import software.xdev.pmd.ui.toolwindow.node.other.NodeErrorAdapter;


public class ErrorInFileNode extends ErrorNode implements HasPositionInFile
{
	private final Supplier<FilePosition> filePositionSupplier;
	
	public ErrorInFileNode(final NodeErrorAdapter errorAdapter, final PsiFile psiFile)
	{
		super(errorAdapter);
		this.filePositionSupplier = Suppliers.memoize(() -> new FilePosition(psiFile));
	}
	
	@Override
	public Supplier<FilePosition> filePositionSupplier()
	{
		return this.filePositionSupplier;
	}
}
