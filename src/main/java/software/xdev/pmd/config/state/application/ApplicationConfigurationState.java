package software.xdev.pmd.config.state.application;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.annotations.Tag;


@State(
	name = "PMD-X-Application",
	storages = {@Storage(value = "pmd-x-app.xml", roamingType = RoamingType.DISABLED)}
)
public class ApplicationConfigurationState
	implements PersistentStateComponent<ApplicationConfigurationState.ApplicationSettings>
{
	private ApplicationSettings applicationSettings = new ApplicationSettings();
	
	@Nullable
	public String getArtifactRepositoryBaseUrlOverride()
	{
		return this.applicationSettings.artifactRepositoryBaseUrlOverride;
	}
	
	public void setArtifactRepositoryBaseUrlOverride(@Nullable final String artifactRepositoryBaseUrlOverride)
	{
		this.applicationSettings.artifactRepositoryBaseUrlOverride = artifactRepositoryBaseUrlOverride;
	}
	
	@Override
	@NotNull
	public ApplicationSettings getState()
	{
		return this.applicationSettings;
	}
	
	@Override
	public void loadState(@NotNull final ApplicationSettings sourceApplicationSettings)
	{
		this.applicationSettings = sourceApplicationSettings;
	}
	
	public static class ApplicationSettings
	{
		@Tag
		String artifactRepositoryBaseUrlOverride;
	}
}
