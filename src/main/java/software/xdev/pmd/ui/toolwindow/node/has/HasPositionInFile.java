package software.xdev.pmd.ui.toolwindow.node.has;

import java.util.function.Supplier;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;

import software.xdev.pmd.ui.toolwindow.node.other.FilePosition;


public interface HasPositionInFile extends HasNavigatable
{
	Supplier<FilePosition> filePositionSupplier();
	
	@Override
	default void navigate(final boolean requestFocus)
	{
		final FilePosition filePosition = this.filePositionSupplier().get();
		final PsiFile psiFile = filePosition.psiFile();
		final VirtualFile virtualFile = psiFile.getVirtualFile();
		if(virtualFile != null)
		{
			final Project project = psiFile.getProject();
			FileEditorManager.getInstance(project).openTextEditor(
				new OpenFileDescriptor(
					project,
					virtualFile,
					filePosition.beginLineIndex(),
					filePosition.beginColumnIndex()
				),
				requestFocus);
		}
	}
	
	@Override
	default boolean canNavigate()
	{
		return true;
	}
	
	@Override
	default boolean canNavigateToSource()
	{
		return true;
	}
}
