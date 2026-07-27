package software.xdev.pmd.config.state.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.XCollection;

import software.xdev.pmd.config.PluginConfigurationManager;
import software.xdev.pmd.model.config.ConfigurationLocation;


/**
 * A manager for CheckStyle module configuration.
 */
@State(
	name = ModuleConfigurationState.ID_MODULE_PLUGIN,
	storages = {@Storage(StoragePathMacros.MODULE_FILE)}
)
public final class ModuleConfigurationState
	implements PersistentStateComponent<ModuleConfigurationState.ModuleSettings>
{
	public static final String ID_MODULE_PLUGIN = "PMD-X-Module";
	
	private final Module module;
	
	private SortedSet<String> activeLocationIds;
	private boolean excludedFromScan;
	
	public ModuleConfigurationState(@NotNull final Module module)
	{
		this.module = module;
	}
	
	public void setActiveLocationIds(@NotNull final SortedSet<String> activeLocationIds)
	{
		this.activeLocationIds = activeLocationIds;
	}
	
	@NotNull
	public SortedSet<String> getActiveLocationIds()
	{
		return Objects.requireNonNullElseGet(this.activeLocationIds, TreeSet::new);
	}
	
	public void setExcluded(final boolean excluded)
	{
		this.excludedFromScan = excluded;
	}
	
	public boolean isExcluded()
	{
		return this.excludedFromScan;
	}
	
	public boolean isUsingModuleConfiguration()
	{
		return !this.getActiveLocationIds().isEmpty();
	}
	
	private PluginConfigurationManager configurationManager()
	{
		return this.module.getProject().getService(PluginConfigurationManager.class);
	}
	
	private List<ConfigurationLocation> configurationLocations()
	{
		return new ArrayList<>(this.configurationManager().getCurrent().locations());
	}
	
	@Override
	@NotNull
	public ModuleSettings getState()
	{
		final ModuleSettings settings = new ModuleSettings();
		settings.useLatestSerialisationFormat();
		settings.setActiveLocationIds(Objects.requireNonNullElseGet(this.activeLocationIds, TreeSet::new));
		settings.setExcludeFromScan(this.excludedFromScan);
		return settings;
	}
	
	@Override
	public void loadState(@NotNull final ModuleSettings moduleSettings)
	{
		this.activeLocationIds = moduleSettings.getActiveLocationIds(
			this.module.getProject(),
			this.configurationLocations());
		this.excludedFromScan = moduleSettings.isExcludeFromScan();
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(o == null || this.getClass() != o.getClass())
		{
			return false;
		}
		final ModuleConfigurationState that = (ModuleConfigurationState)o;
		return this.excludedFromScan == that.excludedFromScan && Objects.equals(this.module, that.module)
			&& Objects.equals(this.activeLocationIds, that.activeLocationIds);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(this.module, this.activeLocationIds, this.excludedFromScan);
	}
	
	static class ModuleSettings
	{
		@Attribute
		private String serialisationVersion;
		
		@XCollection
		private List<String> activeLocationsIds;
		@Tag
		private boolean excludeFromScan;
		
		@NotNull
		static ModuleSettings create(final Map<String, String> configuration)
		{
			return new ModuleSettings();
		}
		
		void setActiveLocationIds(@NotNull final SortedSet<String> newActiveLocationIds)
		{
			this.activeLocationsIds = new ArrayList<>(newActiveLocationIds);
		}
		
		void setExcludeFromScan(final boolean excludeFromScan)
		{
			this.excludeFromScan = excludeFromScan;
		}
		
		@NotNull
		SortedSet<String> getActiveLocationIds(
			@NotNull final Project project,
			@NotNull final List<ConfigurationLocation> locations)
		{
			return new TreeSet<>(Objects.requireNonNullElse(this.activeLocationsIds, Collections.emptyList()));
		}
		
		boolean isExcludeFromScan()
		{
			return this.excludeFromScan;
		}
		
		void useLatestSerialisationFormat()
		{
			this.serialisationVersion = "1";
		}
	}
}
