package software.xdev.pmd.ui.toolwindow.node.has;

import java.util.function.Supplier;

import software.xdev.pmd.ui.toolwindow.FilePosition;


public interface HasPositionInFile
{
	Supplier<FilePosition> filePositionSupplier();
}
