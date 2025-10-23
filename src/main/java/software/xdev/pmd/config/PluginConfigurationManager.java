package software.xdev.pmd.config;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.project.Project;

import software.xdev.pmd.config.state.project.ProjectConfigurationState;


public class PluginConfigurationManager
{
	private final Project project;
	
	public PluginConfigurationManager(@NotNull final Project project)
	{
		this.project = project;
	}
	
	@NotNull
	public PluginConfiguration getCurrent()
	{
		final PluginConfigurationBuilder defaultConfig = PluginConfigurationBuilder.defaultConfiguration(this.project);
		return this.projectConfigurationState()
			.populate(defaultConfig)
			.build();
	}
	
	public void setCurrent(@NotNull final PluginConfiguration updatedConfiguration)
	{
		this.projectConfigurationState().setCurrentConfig(updatedConfiguration);
	}
	
	private ProjectConfigurationState projectConfigurationState()
	{
		return this.project.getService(ProjectConfigurationState.class);
	}
}
