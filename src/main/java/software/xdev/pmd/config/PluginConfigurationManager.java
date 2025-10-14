package software.xdev.pmd.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.project.Project;


public class PluginConfigurationManager
{
	private final List<ConfigurationListener> configurationListeners = Collections.synchronizedList(new ArrayList<>());
	
	private final Project project;
	
	public PluginConfigurationManager(@NotNull final Project project)
	{
		this.project = project;
	}
	
	public void addConfigurationListener(final ConfigurationListener configurationListener)
	{
		if(configurationListener != null)
		{
			this.configurationListeners.add(configurationListener);
		}
	}
	
	private void fireConfigurationChanged()
	{
		synchronized(this.configurationListeners)
		{
			for(final ConfigurationListener configurationListener : this.configurationListeners)
			{
				configurationListener.configurationChanged();
			}
		}
	}
	
	public void disableActiveConfiguration()
	{
		this.setCurrent(
			PluginConfigurationBuilder.from(this.getCurrent())
				.withActiveLocationIds(new TreeSet<>())
				.build(), true);
	}
	
	@NotNull
	public PluginConfiguration getCurrent()
	{
		final PluginConfigurationBuilder defaultConfig = PluginConfigurationBuilder.defaultConfiguration(this.project);
		return this.projectConfigurationState()
			.populate(defaultConfig)
			.build();
	}
	
	public void setCurrent(@NotNull final PluginConfiguration updatedConfiguration, final boolean fireEvents)
	{
		this.projectConfigurationState().setCurrentConfig(updatedConfiguration);
		if(fireEvents)
		{
			this.fireConfigurationChanged();
		}
	}
	
	private ProjectConfigurationState projectConfigurationState()
	{
		return this.project.getService(ProjectConfigurationState.class);
	}
}
