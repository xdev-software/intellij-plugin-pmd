package software.xdev.pmd.config;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.project.Project;

import software.xdev.pmd.analysis.ProjectRulesetClasspathManager;
import software.xdev.pmd.config.state.project.ProjectConfigurationState;
import software.xdev.pmd.config.state.project.ProjectSettingsState;


public class PluginConfigurationManager
{
	private final Project project;
	
	private ProjectSettingsState lastProjectSettingsState;
	private PluginConfiguration lastPluginConfiguration;
	
	public PluginConfigurationManager(@NotNull final Project project)
	{
		this.project = project;
	}
	
	@NotNull
	public PluginConfiguration getCurrent()
	{
		final ProjectConfigurationState projectConfigurationState = this.projectConfigurationState();
		final ProjectSettingsState currentProjectSettingsState = projectConfigurationState.getState();
		if(!Objects.equals(this.lastProjectSettingsState, currentProjectSettingsState))
		{
			this.setLastPluginConfiguration(
				currentProjectSettingsState,
				projectConfigurationState
					.populate(new PluginConfigurationBuilder(this.project))
					.build());
		}
		
		return this.lastPluginConfiguration;
	}
	
	public void setCurrent(@NotNull final PluginConfiguration updatedConfiguration)
	{
		final ProjectConfigurationState projectConfigurationState = this.projectConfigurationState();
		
		this.setLastPluginConfiguration(projectConfigurationState.getState(), updatedConfiguration);
		
		projectConfigurationState.setCurrentConfig(updatedConfiguration);
	}
	
	private void setLastPluginConfiguration(
		final ProjectSettingsState projectSettingsState,
		@NotNull final PluginConfiguration pluginConfig)
	{
		this.lastProjectSettingsState = projectSettingsState;
		this.lastPluginConfiguration = pluginConfig;
		
		// Update the classpath information
		this.project.getService(ProjectRulesetClasspathManager.class).configure(pluginConfig.thirdPartyCPLocations());
	}
	
	private ProjectConfigurationState projectConfigurationState()
	{
		return this.project.getService(ProjectConfigurationState.class);
	}
}
