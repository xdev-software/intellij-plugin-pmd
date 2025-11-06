package software.xdev.pmd.ui.toolwindow.analysis.report;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;

import software.xdev.pmd.currentfile.CombinedPMDAnalysisResult;
import software.xdev.pmd.ui.toolwindow.PMDToolWindowFactory;


public class ReportViewManager
{
	private static final DateTimeFormatter REPORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
	
	@NotNull
	private final Project project;
	
	public ReportViewManager(@NotNull final Project project)
	{
		this.project = project;
	}
	
	public void displayNewReport(final CombinedPMDAnalysisResult result, final AnActionEvent triggeringEvent)
	{
		ApplicationManager.getApplication().invokeLater(() ->
			Optional.ofNullable(ToolWindowManager.getInstance(this.project)
					.getToolWindow(PMDToolWindowFactory.TOOL_WINDOW_ID))
				.ifPresent(toolWindow -> this.displayNewReportInToolWindow(result, triggeringEvent, toolWindow)));
	}
	
	private void displayNewReportInToolWindow(
		final CombinedPMDAnalysisResult result,
		final AnActionEvent triggeringEvent,
		final ToolWindow toolWindow)
	{
		toolWindow.activate(null);
		
		final ContentManager contentManager = toolWindow.getContentManager();
		final ReportPanel reportPanel = new ReportPanel(this.project, result, triggeringEvent);
		final Content reportContent = contentManager.getFactory().createContent(
			reportPanel,
			"Report " + LocalDateTime.now().format(REPORT_DATE_FORMATTER),
			true);
		reportContent.setDisposer(reportPanel);
		reportContent.setCloseable(true);
		contentManager.addContent(reportContent);
		
		toolWindow.show(() -> contentManager.setSelectedContent(reportContent));
	}
}
