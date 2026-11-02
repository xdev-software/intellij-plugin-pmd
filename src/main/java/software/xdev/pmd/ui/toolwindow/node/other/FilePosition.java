package software.xdev.pmd.ui.toolwindow.node.other;

import com.intellij.psi.PsiFile;

import net.sourceforge.pmd.lang.document.FileLocation;


public record FilePosition(
	PsiFile psiFile,
	int beginLineIndex,
	int beginColumnIndex
)
{
	public FilePosition(final PsiFile psiFile)
	{
		this(psiFile, 0, 0);
	}
	
	public FilePosition(final PsiFile psiFile, final FileLocation location)
	{
		this(
			psiFile,
			location.getStartLine() - 1,
			location.getStartColumn() - 1);
	}
}
