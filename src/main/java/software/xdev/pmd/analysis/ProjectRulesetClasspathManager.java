package software.xdev.pmd.analysis;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Objects;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;

import software.xdev.pmd.model.config.thirdpartycplocation.ThirdPartyCPLocation;


public class ProjectRulesetClasspathManager implements Disposable
{
	private static final ClassLoader DEFAULT_CL = ProjectRulesetClasspathManager.class.getClassLoader();
	private static final Logger LOG = Logger.getInstance(ProjectRulesetClasspathManager.class);
	
	private List<ThirdPartyCPLocation> cachedKey;
	private URLClassLoader classLoader;
	
	public void configure(final List<ThirdPartyCPLocation> locations)
	{
		if(!Objects.equals(this.cachedKey, locations))
		{
			this.closeCurrentClassLoader();
			if(!locations.isEmpty())
			{
				this.classLoader = new URLClassLoader(
					locations.stream().map(ThirdPartyCPLocation::url).toArray(URL[]::new),
					DEFAULT_CL);
			}
			
			this.cachedKey = locations;
		}
	}
	
	public ClassLoader getClassLoader()
	{
		return this.classLoader != null ? this.classLoader : DEFAULT_CL;
	}
	
	private void closeCurrentClassLoader()
	{
		closeClassLoader(this.classLoader);
		this.classLoader = null;
	}
	
	@Override
	public void dispose()
	{
		this.closeCurrentClassLoader();
	}
	
	private static void closeClassLoader(final URLClassLoader classLoader)
	{
		if(classLoader != null)
		{
			try
			{
				classLoader.close();
			}
			catch(final IOException e)
			{
				LOG.warn("Failed to close classloader", e);
			}
		}
	}
}
