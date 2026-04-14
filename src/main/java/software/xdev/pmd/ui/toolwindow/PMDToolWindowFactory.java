package software.xdev.pmd.ui.toolwindow;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;

import software.xdev.pmd.ui.toolwindow.analysis.currentfile.CurrentFilePanel;


public class PMDToolWindowFactory implements ToolWindowFactory
{
	public static final String TOOL_WINDOW_ID = "PMD";
	
	@Override
	public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow)
	{
		// Run on UI Thread
		ApplicationManager.getApplication().invokeLater(() -> {
			final ContentManager contentManager = toolWindow.getContentManager();
			
			final CurrentFilePanel currentFilePanel = new CurrentFilePanel(project);
			final Content currentFileContent = contentManager.getFactory().createContent(
				currentFilePanel,
				"Current File",
				false);
			currentFileContent.setDisposer(currentFilePanel);
			currentFileContent.setCloseable(false);
			contentManager.addContent(currentFileContent, 0);
			
			toolWindow.setType(ToolWindowType.DOCKED, null);
		});
	}
}
