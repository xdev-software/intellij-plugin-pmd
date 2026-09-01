package software.xdev.pmd.action.analysis;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;


public interface ReplayableAnalysisInfo
{
	Project getProject();
	
	VirtualFile[] getFiles(Project project);
}
