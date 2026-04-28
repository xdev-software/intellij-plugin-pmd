package software.xdev.pmd.ui.toolwindow.node;

import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jetbrains.annotations.NotNull;

import one.util.streamex.StreamEx;
import software.xdev.pmd.ui.toolwindow.node.has.HasNavigatable;
import software.xdev.pmd.ui.toolwindow.node.render.NodeCellRenderer;


public abstract class BaseNode extends DefaultMutableTreeNode implements HasNavigatable
{
	public abstract void render(@NotNull NodeCellRenderer renderer);
	
	public void executeRecursive(@NotNull final Consumer<BaseNode> action)
	{
		StreamEx.of(this.children())
			.filter(BaseNode.class::isInstance)
			.map(BaseNode.class::cast)
			.forEach(n -> n.executeRecursive(action));
		action.accept(this);
	}
	
	protected <T> Stream<T> childrenStream(final Class<T> nodeClass)
	{
		return StreamEx.of(this.children())
			.filter(nodeClass::isInstance)
			.map(nodeClass::cast);
	}
	
	protected <T> int childrenSum(final Class<T> nodeClass, final ToIntFunction<T> toIntMapper)
	{
		return this.childrenStream(nodeClass)
			.mapToInt(toIntMapper)
			.sum();
	}
	
	public void update()
	{
		// Default - NOOP
	}
}
