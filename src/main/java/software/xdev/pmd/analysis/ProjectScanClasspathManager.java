package software.xdev.pmd.analysis;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Objects;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;

import software.xdev.pmd.config.plugin.ThirdPartyClasspathConfigContainer;


public class ProjectScanClasspathManager implements Disposable
{
	private static final ClassLoader DEFAULT_CL = ProjectScanClasspathManager.class.getClassLoader();
	private static final Logger LOG = Logger.getInstance(ProjectScanClasspathManager.class);
	
	private ThirdPartyClasspathConfigContainer cachedConfigContainerKey;
	private URLClassLoader classLoader;
	
	public void configure(final ThirdPartyClasspathConfigContainer configContainer)
	{
		if(!Objects.equals(this.cachedConfigContainerKey, configContainer))
		{
			this.closeCurrentClassLoader();
			if(!configContainer.urls().isEmpty())
			{
				this.classLoader = new URLClassLoader(configContainer.urls().toArray(URL[]::new), DEFAULT_CL);
			}
			
			this.cachedConfigContainerKey = configContainer;
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
