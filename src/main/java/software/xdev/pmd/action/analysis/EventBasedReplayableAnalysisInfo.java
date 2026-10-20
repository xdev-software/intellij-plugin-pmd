package software.xdev.pmd.action.analysis;

import java.util.function.Function;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;


public class EventBasedReplayableAnalysisInfo implements ReplayableAnalysisInfo
{
	private final AnActionEvent ev;
	private final Function<Project, VirtualFile[]> getFilesFunc;
	
	public EventBasedReplayableAnalysisInfo(
		final AnActionEvent ev,
		final Function<Project, VirtualFile[]> getFilesFunc)
	{
		this.ev = ev;
		this.getFilesFunc = getFilesFunc;
	}
	
	@Override
	public Project getProject()
	{
		return this.ev.getProject();
	}
	
	@Override
	public VirtualFile[] getFiles(final Project project)
	{
		return this.getFilesFunc.apply(project);
	}
	
	public static VirtualFile[] getCommonAncestorsContentRoots(final Project project)
	{
		return VfsUtil.getCommonAncestors(ProjectRootManager.getInstance(project).getContentRoots());
	}
}
