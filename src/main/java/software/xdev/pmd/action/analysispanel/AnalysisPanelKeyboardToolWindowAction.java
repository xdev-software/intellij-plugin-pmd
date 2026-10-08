package software.xdev.pmd.action.analysispanel;

import java.awt.Component;
import java.awt.KeyboardFocusManager;

import javax.swing.SwingUtilities;

import com.intellij.openapi.wm.ToolWindow;


public abstract class AnalysisPanelKeyboardToolWindowAction extends AnalysisPanelKeyboardAction
{
	@Override
	protected boolean shouldAbort(final ToolWindow toolWindow)
	{
		final Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		return focusOwner == null || !SwingUtilities.isDescendingFrom(focusOwner, toolWindow.getComponent());
	}
}
