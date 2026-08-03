package software.xdev.pmd.ui.config.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JComponent;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleConfigurationEditor;

import software.xdev.pmd.config.PluginConfiguration;
import software.xdev.pmd.config.PluginConfigurationManager;
import software.xdev.pmd.config.state.module.ModuleConfigurationState;
import software.xdev.pmd.model.config.ConfigurationLocation;


public class PMDModuleConfigurationEditor implements ModuleConfigurationEditor
{
	private final Module module;
	
	public PMDModuleConfigurationEditor(@NotNull final Module module)
	{
		this.module = module;
	}
	
	private PMDModuleConfigPanel configPanel;
	
	@Override
	public String getDisplayName()
	{
		return "PMD";
	}
	
	@Override
	public String getHelpTopic()
	{
		return null;
	}
	
	@Override
	public JComponent createComponent()
	{
		if(this.configPanel == null)
		{
			this.configPanel = new PMDModuleConfigPanel();
		}
		
		this.reset();
		
		return this.configPanel;
	}
	
	@Override
	public boolean isModified()
	{
		return this.configPanel != null && this.configPanel.isModified();
	}
	
	@Override
	public void apply()
	{
		if(this.configPanel == null)
		{
			return;
		}
		
		final ModuleConfigurationState configuration = this.moduleConfigurationState();
		
		final ConfigurationLocation activeLocation = this.configPanel.getActiveLocation();
		final SortedSet<String> activeLocationIds = new TreeSet<>();
		if(activeLocation != null)
		{
			activeLocationIds.add(activeLocation.getId());
		}
		configuration.setActiveLocationIds(activeLocationIds);
		configuration.setExcluded(this.configPanel.isExcluded());
		
		this.reset();
	}
	
	private ModuleConfigurationState moduleConfigurationState()
	{
		return this.module.getService(ModuleConfigurationState.class);
	}
	
	private PluginConfigurationManager pluginConfigurationManager()
	{
		return this.module.getProject().getService(PluginConfigurationManager.class);
	}
	
	@Override
	public void reset()
	{
		if(this.configPanel == null)
		{
			return;
		}
		
		final ModuleConfigurationState moduleConfiguration = this.moduleConfigurationState();
		final PluginConfiguration pluginConfiguration = this.pluginConfigurationManager().getCurrent();
		
		this.configPanel.setConfigurationLocations(new ArrayList<>(pluginConfiguration.locations()));
		
		if(moduleConfiguration.isExcluded())
		{
			this.configPanel.setExcluded(true);
		}
		else if(moduleConfiguration.isUsingModuleConfiguration())
		{
			this.configPanel.setActiveLocations(moduleConfiguration.getActiveLocationIds().stream()
				.map(pluginConfiguration::getLocationById)
				.filter(Objects::nonNull)
				.toList());
		}
		else
		{
			this.configPanel.setActiveLocations(List.of());
		}
	}
	
	@Override
	public void disposeUIResources()
	{
		this.configPanel = null;
	}
}
