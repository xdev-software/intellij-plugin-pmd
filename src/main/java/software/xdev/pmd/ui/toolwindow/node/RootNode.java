package software.xdev.pmd.ui.toolwindow.node;

import org.jetbrains.annotations.NotNull;

import com.intellij.icons.AllIcons;

import software.xdev.pmd.ui.toolwindow.node.render.NodeCellRenderer;


public class RootNode extends BaseHasViolationSuppressedErrorNode
{
	@Override
	public void render(@NotNull final NodeCellRenderer renderer)
	{
		renderer.setIcon(AllIcons.General.TodoDefault);
		final String s = this.violationsSuppressedErrorToString();
		renderer.append(s.isEmpty() ? "No problems detected" : s);
	}
}
