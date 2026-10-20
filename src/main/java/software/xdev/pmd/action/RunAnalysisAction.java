package software.xdev.pmd.action;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import software.xdev.pmd.action.analysis.ActionFilesAnalyzer;
import software.xdev.pmd.action.analysis.EventBasedReplayableAnalysisInfo;


public class RunAnalysisAction extends AbstractAnAction
{
	@Override
	protected boolean isVisible(final AnActionEvent ev, final Project project)
	{
		final VirtualFile[] files = ev.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
		return files != null && files.length > 0;
	}
	
	@Override
	public void actionPerformed(@NotNull final AnActionEvent ev)
	{
		ApplicationManager.getApplication().getService(ActionFilesAnalyzer.class)
			.analyze(new EventBasedReplayableAnalysisInfo(
				ev,
				project -> ActionPlaces.MAIN_MENU.equals(ev.getPlace())
					? EventBasedReplayableAnalysisInfo.getCommonAncestorsContentRoots(project)
					: ev.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
			));
	}
}
