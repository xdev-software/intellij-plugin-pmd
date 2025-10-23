package software.xdev.pmd.config.state.project;

import static software.xdev.pmd.config.PluginConfigurationBuilder.defaultConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;

import software.xdev.pmd.config.PluginConfiguration;
import software.xdev.pmd.config.PluginConfigurationBuilder;


@State(name = "PMD-X", storages = {@Storage("pmd-x.xml")})
public class ProjectConfigurationState implements PersistentStateComponent<ProjectSettingsState>
{
	private final Project project;
	
	@Nullable
	private ProjectSettingsState projectSettings;
	
	public ProjectConfigurationState(@NotNull final Project project)
	{
		this.project = project;
	}
	
	private ProjectSettingsState defaultProjectSettings()
	{
		return ProjectSettingsState.create(defaultConfiguration(this.project).build());
	}
	
	private ProjectSettingsState projectSettingsOrLoadDefault()
	{
		if(this.projectSettings == null)
		{
			this.projectSettings = this.defaultProjectSettings();
		}
		return this.projectSettings;
	}
	
	@Override
	public ProjectSettingsState getState()
	{
		return this.projectSettingsOrLoadDefault();
	}
	
	@Override
	public void loadState(@NotNull final ProjectSettingsState sourceProjectSettings)
	{
		this.projectSettings = sourceProjectSettings;
	}
	
	@NotNull
	public PluginConfigurationBuilder populate(@NotNull final PluginConfigurationBuilder builder)
	{
		return this.projectSettingsOrLoadDefault().populate(builder, this.project);
	}
	
	public void setCurrentConfig(@NotNull final PluginConfiguration currentPluginConfig)
	{
		this.projectSettings = ProjectSettingsState.create(currentPluginConfig);
	}
}
