package software.xdev.pmd.util.io;

import static java.util.Arrays.asList;

import java.io.File;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;


public class ProjectFilePaths
{
	private static final Logger LOG = Logger.getInstance(ProjectFilePaths.class);
	
	private static final String IDEA_PROJECT_DIR = "$PROJECT_DIR$";
	private static final String LEGACY_PROJECT_DIR = "$PRJ_DIR$";
	
	private final Project project;
	private final char separatorChar;
	private final Function<File, String> absolutePathOf;
	
	public ProjectFilePaths(@NotNull final Project project)
	{
		this(project, File.separatorChar, File::getAbsolutePath);
	}
	
	private ProjectFilePaths(
		@NotNull final Project project,
		final char separatorChar,
		@NotNull final Function<File, String> absolutePathOf)
	{
		this.project = project;
		this.separatorChar = separatorChar;
		this.absolutePathOf = absolutePathOf;
	}
	
	@Nullable
	public String makeProjectRelative(@Nullable final String path)
	{
		if(path == null || this.project.isDefault())
		{
			return path;
		}
		
		final File projectPath = this.projectPath();
		if(projectPath == null)
		{
			LOG.debug("Couldn't find project path, returning full path: " + path);
			return path;
		}
		
		try
		{
			final String basePath = this.absolutePathOf.apply(projectPath) + this.separatorChar;
			return basePath + FilePaths.relativePath(path, basePath, String.valueOf(this.separatorChar));
		}
		catch(final FilePaths.PathResolutionException e)
		{
			LOG.debug("No common path was found between " + path + " and " + projectPath.getAbsolutePath());
			return path;
		}
		catch(final Exception e)
		{
			LOG.warn("Failed to make relative: " + path, e);
			return path;
		}
	}
	
	@Nullable
	public String tokenise(@Nullable final String fsPath)
	{
		if(fsPath == null)
		{
			return null;
		}
		
		if(this.project.isDefault())
		{
			if(new File(fsPath).exists() || fsPath.startsWith(IDEA_PROJECT_DIR))
			{
				return this.toUnixPath(fsPath);
			}
			else
			{
				return IDEA_PROJECT_DIR + this.toUnixPath(this.separatorChar + fsPath);
			}
		}
		
		final File projectPath = this.projectPath();
		if(projectPath != null && fsPath.startsWith(this.absolutePathOf.apply(projectPath) + this.separatorChar))
		{
			return IDEA_PROJECT_DIR
				+ this.toUnixPath(fsPath.substring(this.absolutePathOf.apply(projectPath).length()));
		}
		
		return this.toUnixPath(fsPath);
	}
	
	@Nullable
	public String detokenize(@Nullable final String tokenisedPath)
	{
		if(tokenisedPath == null)
		{
			return null;
		}
		
		String detokenisedPath = this.replaceProjectToken(tokenisedPath);
		
		if(detokenisedPath == null)
		{
			detokenisedPath = this.toSystemPath(tokenisedPath);
		}
		return detokenisedPath;
	}
	
	private String replaceProjectToken(final String path)
	{
		for(final String projectDirToken : asList(IDEA_PROJECT_DIR, LEGACY_PROJECT_DIR))
		{
			final int prefixLocation = path.indexOf(projectDirToken);
			if(prefixLocation >= 0)
			{
				final File projectPath = this.projectPath();
				if(projectPath != null)
				{
					final String projectRelativePath =
						this.toSystemPath(path.substring(prefixLocation + projectDirToken.length()));
					final String completePath = projectPath + File.separator + projectRelativePath;
					return this.absolutePathOf.apply(new File(completePath));
				}
				else
				{
					LOG.warn("Could not detokenize path as project dir is unset: " + path);
				}
			}
		}
		return null;
	}
	
	private String toUnixPath(final String systemPath)
	{
		if(this.separatorChar == '/')
		{
			return systemPath;
		}
		return systemPath.replace(this.separatorChar, '/');
	}
	
	private String toSystemPath(final String unixPath)
	{
		if(this.separatorChar == '/')
		{
			return unixPath;
		}
		return unixPath.replace('/', this.separatorChar);
	}
	
	@Nullable
	private File projectPath()
	{
		try
		{
			final VirtualFile baseDir = ProjectUtil.guessProjectDir(this.project);
			if(baseDir == null)
			{
				return null;
			}
			
			return new File(baseDir.getPath());
		}
		catch(final Exception e)
		{
			// IDEA 10.5.2 sometimes throws an AssertionException in project.getBaseDir()
			LOG.debug("Couldn't retrieve base location", e);
			return null;
		}
	}
}
