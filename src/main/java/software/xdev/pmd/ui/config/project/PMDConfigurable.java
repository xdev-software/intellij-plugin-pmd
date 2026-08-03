package software.xdev.pmd.ui.config.project;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;

import software.xdev.pmd.config.PluginConfiguration;
import software.xdev.pmd.config.PluginConfigurationBuilder;
import software.xdev.pmd.config.PluginConfigurationManager;


/**
 * The "configurable component" required by IntelliJ IDEA to provide a Swing form for inclusion into the 'Settings'
 * dialog. Registered in {@code plugin.xml} as a {@code projectConfigurable} extension.
 */
public class PMDConfigurable implements Configurable
{
	private final PMDConfigPanel configPanel;
	private final PluginConfigurationManager pluginConfigurationManager;
	
	PMDConfigurable(@NotNull final Project project)
	{
		this.pluginConfigurationManager = project.getService(PluginConfigurationManager.class);
		
		// Default project (start screen) is not supported!
		this.configPanel = !project.isDefault() ? new PMDConfigPanel(project) : null;
	}
	
	@Override
	public String getDisplayName()
	{
		return "PMD";
	}
	
	@Override
	public JComponent createComponent()
	{
		if(this.configPanel == null)
		{
			return new JLabel("Project configuration not available");
		}
		
		this.reset();
		return this.configPanel;
	}
	
	private PluginConfiguration getConfigPanelPluginConfig()
	{
		return PluginConfigurationBuilder.copy(this.configPanel.getPluginConfiguration());
	}
	
	@Override
	public boolean isModified()
	{
		if(this.configPanel == null)
		{
			return false;
		}
		
		return !this.pluginConfigurationManager.getCurrent() // Old
			.isIdentical(this.getConfigPanelPluginConfig()); // New
	}
	
	@Override
	public void apply()
	{
		if(this.configPanel == null)
		{
			return;
		}
		
		this.pluginConfigurationManager.setCurrent(this.getConfigPanelPluginConfig());
	}
	
	@Override
	public void reset()
	{
		if(this.configPanel == null)
		{
			return;
		}
		
		this.configPanel.showPluginConfiguration(this.pluginConfigurationManager.getCurrent());
	}
}
