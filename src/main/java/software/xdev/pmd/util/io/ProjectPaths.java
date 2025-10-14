package software.xdev.pmd.util.io;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;


public class ProjectPaths
{
	@Nullable
	public VirtualFile projectPath(@NotNull final Project project)
	{
		return ProjectUtil.guessProjectDir(project);
	}
	
	@Nullable
	public VirtualFile modulePath(@NotNull final Module module)
	{
		return ProjectUtil.guessModuleDir(module);
	}
}
