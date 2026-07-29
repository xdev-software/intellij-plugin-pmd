package software.xdev.pmd.util.io;

import java.io.File;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;


public class ProjectFilePaths
{
	private final Project project;
	private final char separatorChar;
	
	public ProjectFilePaths(@NotNull final Project project)
	{
		this(project, File.separatorChar);
	}
	
	private ProjectFilePaths(
		@NotNull final Project project,
		final char separatorChar)
	{
		this.project = project;
		this.separatorChar = separatorChar;
	}
	
	public String makeProjectRelativePathAbsolute(final String path)
	{
		if(path == null || this.project.isDefault())
		{
			throw new IllegalArgumentException("Invalid path or project");
		}
		
		final VirtualFile projectDir = ProjectUtil.guessProjectDir(this.project);
		if(projectDir == null)
		{
			throw new IllegalStateException("Unable to guess directory of project " + this.project);
		}
		
		return projectDir.toNioPath().toAbsolutePath().toString() + this.separatorChar + path;
	}
	
	public String toUnixPath(final String systemPath)
	{
		if(this.separatorChar == '/')
		{
			return systemPath;
		}
		return systemPath.replace(this.separatorChar, '/');
	}
	
	public String toSystemPath(final String unixPath)
	{
		if(this.separatorChar == '/')
		{
			return unixPath;
		}
		return unixPath.replace('/', this.separatorChar);
	}
}
