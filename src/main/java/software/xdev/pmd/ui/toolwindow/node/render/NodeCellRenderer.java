package software.xdev.pmd.ui.toolwindow.node.render;

import javax.swing.JTree;

import org.jetbrains.annotations.NotNull;

import com.intellij.ui.ColoredTreeCellRenderer;

import software.xdev.pmd.ui.toolwindow.node.BaseNode;


public class NodeCellRenderer extends ColoredTreeCellRenderer
{
	@Override
	public void customizeCellRenderer(
		@NotNull final JTree tree,
		final Object value,
		final boolean selected,
		final boolean expanded,
		final boolean leaf,
		final int row,
		final boolean hasFocus)
	{
		if(value instanceof final BaseNode baseNode)
		{
			baseNode.render(this);
		}
	}
}
