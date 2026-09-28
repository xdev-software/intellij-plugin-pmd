package software.xdev.pmd.ui.toolwindow.node;

import org.jetbrains.annotations.NotNull;

import com.intellij.icons.AllIcons;

import software.xdev.pmd.ui.toolwindow.node.has.HasErrorAdapter;
import software.xdev.pmd.ui.toolwindow.node.has.HasErrorCount;
import software.xdev.pmd.ui.toolwindow.node.other.NodeErrorAdapter;
import software.xdev.pmd.ui.toolwindow.node.render.NodeCellRenderer;


public class ErrorNode extends BaseNode implements HasErrorCount, HasErrorAdapter
{
	private final NodeErrorAdapter errorAdapter;
	
	public ErrorNode(final NodeErrorAdapter errorAdapter)
	{
		this.errorAdapter = errorAdapter;
	}
	
	@Override
	public void render(@NotNull final NodeCellRenderer renderer)
	{
		renderer.setIcon(AllIcons.General.Error);
		renderer.append(this.errorAdapter.summary());
	}
	
	@Override
	public int errorCount()
	{
		return 1;
	}
	
	@Override
	public NodeErrorAdapter errorAdapter()
	{
		return this.errorAdapter;
	}
}
