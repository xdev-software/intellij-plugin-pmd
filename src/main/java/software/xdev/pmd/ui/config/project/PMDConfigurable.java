package software.xdev.pmd.ui.config.project;

import javax.swing.JComponent;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.diagnostic.Logger;
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
	private static final Logger LOG = Logger.getInstance(PMDConfigurable.class);
	
	private final PMDConfigPanel configPanel;
	private final PluginConfigurationManager pluginConfigurationManager;
	
	PMDConfigurable(@NotNull final Project project)
	{
		this.pluginConfigurationManager = project.getService(PluginConfigurationManager.class);
		
		this.configPanel = new PMDConfigPanel(project);
	}
	
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
		this.reset();
		return this.configPanel;
	}
	
	@Override
	public boolean isModified()
	{
		final PluginConfiguration oldConfig = this.pluginConfigurationManager.getCurrent();
		final PluginConfiguration newConfig = PluginConfigurationBuilder
			.from(this.configPanel.getPluginConfiguration())
			.build();
		
		final boolean result = !oldConfig.hasChangedFrom(newConfig);
		if(LOG.isDebugEnabled())
		{
			LOG.debug("Has config changed? " + result);
		}
		return result;
	}
	
	@Override
	public void apply()
	{
		final PluginConfiguration newConfig = PluginConfigurationBuilder
			.from(this.configPanel.getPluginConfiguration())
			.build();
		this.pluginConfigurationManager.setCurrent(newConfig, true);
	}
	
	@Override
	public void reset()
	{
		final PluginConfiguration pluginConfig = this.pluginConfigurationManager.getCurrent();
		this.configPanel.showPluginConfiguration(pluginConfig);
	}
	
	@Override
	public void disposeUIResources()
	{
		// do nothing
	}
}
