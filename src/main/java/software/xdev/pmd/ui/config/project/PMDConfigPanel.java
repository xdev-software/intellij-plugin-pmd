package software.xdev.pmd.ui.config.project;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jetbrains.annotations.NotNull;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.util.ui.JBUI;

import software.xdev.pmd.config.PluginConfiguration;
import software.xdev.pmd.config.PluginConfigurationBuilder;
import software.xdev.pmd.config.plugin.PatternContainer;
import software.xdev.pmd.config.plugin.ThirdPartyClasspathConfigContainer;
import software.xdev.pmd.model.config.rulesetlocation.ConfigurationLocation;
import software.xdev.pmd.model.scope.ScanScope;
import software.xdev.pmd.ui.config.project.components.exclusion.FileMaskPanelManager;
import software.xdev.pmd.ui.config.project.components.rulefilelocation.LocationPanelManager;
import software.xdev.pmd.ui.config.project.components.thirdpartyclasspath.ThirdPartyClasspathPanelManager;


/**
 * Provides a configuration panel (dialog) for project-level configuration.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class PMDConfigPanel extends JPanel
{
	private static final Insets COMPONENT_INSETS = JBUI.insets(4);
	
	private final JLabel lblScopeDropdown = new JLabel("Scan Scope:");
	private final ComboBox<ScanScope> cbScope = new ComboBox<>(ScanScope.values());
	private final JBCheckBox chbxUseSingleThread = new JBCheckBox("Use single thread");
	private final JBCheckBox chbxShowSuppressedWarnings = new JBCheckBox("Show suppressed warnings");
	private final JBCheckBox chbxUseCacheFile = new JBCheckBox("Use cache file");
	private final JBCheckBox chbxImportSettingsFromMaven = new JBCheckBox("Import settings from Maven");
	
	private final LocationPanelManager rulefileLocationPanelManager;
	
	private final FileMaskPanelManager exclusionPanelManager = new FileMaskPanelManager(
		"Exclusions",
		"Nothing excluded",
		"Add exclusion",
		"Edit exclusion",
		"""
			<html><body>
			<p>Ignores certain files (patterns).
			<p>(use case sensitive Java regular expression that matches the end of the full file path)</p>
			<ul>
			<li><strong>Ignore\\.java</strong>              (exclude file 'Ignore.java' in all folders)</li>
			<li><strong>.*\\.properties</strong>            (exclude all '.properties' in all folders)</li>
			<li><strong>src/Ignore\\.java</strong>          (exclude file 'Ignore.java' in 'src' folders)</li>
			<li><strong>ignore/.*</strong>                  (exclude folder 'ignore' recursively)</li>
			<li><strong>myProject/Ignore.md</strong>        (exclude file 'Ignore.md' in project 'myProject')</li>
			</ul>
			</body></html>"""
	);
	
	private final ThirdPartyClasspathPanelManager thirdPartyClasspathPanelManager;
	
	final Project project;
	
	public PMDConfigPanel(@NotNull final Project project)
	{
		super(new BorderLayout());
		
		this.project = project;
		
		this.rulefileLocationPanelManager = new LocationPanelManager(project, this);
		this.thirdPartyClasspathPanelManager = new ThirdPartyClasspathPanelManager(project, this);
		
		this.initialise();
	}
	
	private void initialise()
	{
		this.add(this.buildConfigPanel(), BorderLayout.CENTER);
	}
	
	private JPanel buildConfigPanel()
	{
		final JPanel configFilePanel = new JPanel(new GridBagLayout());
		configFilePanel.setOpaque(false);
		
		configFilePanel.add(this.lblScopeDropdown, this.createDefaultGridBagConstraints(0, 0, 1));
		configFilePanel.add(this.cbScope, this.createDefaultGridBagConstraints(1, 0, 1));
		configFilePanel.add(
			this.wrapWithInfoIcon(
				this.chbxUseSingleThread,
				"Analysis will be a lot slower but might be more stable"),
			this.createDefaultGridBagConstraints(2, 0, 2));
		
		configFilePanel.add(this.chbxShowSuppressedWarnings, this.createDefaultGridBagConstraints(0, 1, 2));
		configFilePanel.add(
			this.wrapWithInfoIcon(
				this.chbxUseCacheFile,
				"Repeated analysis will be a lot faster.<br>"
					+ "Only disable this when you have problems with cache file corruption"),
			this.createDefaultGridBagConstraints(2, 1, 2));
		
		configFilePanel.add(
			this.wrapWithInfoIcon(
				this.chbxImportSettingsFromMaven,
				"Experimental/Best effort - not all options/usage possibilities of the maven plugin are supported.<br>"
					+ "Importing only happens during maven project syncing.<br/>"
					+ "It's recommended to only enable this when importing changed configuration."),
			this.createDefaultGridBagConstraints(0, 2, 2));
		
		this.addPanel(
			configFilePanel,
			this.rulefileLocationPanelManager.buildPanel(),
			3,
			1.0,
			300);
		
		this.addPanel(
			configFilePanel,
			this.exclusionPanelManager.getPanel(),
			4,
			0.1,
			150);
		
		this.addPanel(
			configFilePanel,
			this.thirdPartyClasspathPanelManager.buildPanel(),
			5,
			0.5,
			150);
		
		return configFilePanel;
	}
	
	private JPanel wrapWithInfoIcon(final JComponent component, final String infoText)
	{
		final JBLabel indicator = new JBLabel(AllIcons.General.Information);
		// Warning 'setToolTipText(HtmlChunk)' can be ignored as this is a hardcoded string
		indicator.setToolTipText(infoText);
		
		final JPanel hl = new JPanel(new HorizontalLayout(5));
		hl.add(component);
		hl.add(indicator);
		return hl;
	}
	
	private GridBagConstraints createDefaultGridBagConstraints(final int gridX, final int gridY, final int gridWidth)
	{
		return new GridBagConstraints(
			gridX,
			gridY,
			gridWidth,
			1,
			gridWidth > 1 ? 1.0 : 0.0,
			0.0,
			GridBagConstraints.WEST,
			GridBagConstraints.HORIZONTAL,
			COMPONENT_INSETS,
			0,
			0);
	}
	
	private GridBagConstraints createFullWidthGridBagConstraints(final int gridY, final double weighty)
	{
		return new GridBagConstraints(
			0,
			gridY,
			4,
			1,
			1.0,
			weighty,
			GridBagConstraints.WEST,
			GridBagConstraints.BOTH,
			COMPONENT_INSETS,
			0,
			0);
	}
	
	private void addPanel(
		final JPanel panel,
		final JPanel panelToAdd,
		final int gridY,
		final double weighty,
		final int preferredHeight
	)
	{
		panelToAdd.setPreferredSize(new Dimension(Integer.MAX_VALUE, preferredHeight));
		panel.add(
			panelToAdd,
			this.createFullWidthGridBagConstraints(gridY, weighty));
	}
	
	public void showPluginConfiguration(@NotNull final PluginConfiguration pluginConfig)
	{
		this.cbScope.setSelectedItem(pluginConfig.scanScope());
		this.chbxUseSingleThread.setSelected(pluginConfig.useSingleThread());
		this.chbxShowSuppressedWarnings.setSelected(pluginConfig.showSuppressedWarnings());
		this.chbxUseCacheFile.setSelected(pluginConfig.useCacheFile());
		this.chbxImportSettingsFromMaven.setSelected(pluginConfig.importSettingsFromMaven());
		this.rulefileLocationPanelManager.locationModel().setLocations(new ArrayList<>(pluginConfig.locations()));
		this.rulefileLocationPanelManager.locationModel().setActiveLocations(pluginConfig.getActiveLocations());
		this.exclusionPanelManager.update(pluginConfig.projectRelativeFileExclusions().stream()
			.map(PatternContainer::patternString)
			.collect(Collectors.toCollection(TreeSet::new)));
		this.thirdPartyClasspathPanelManager.setThirdPartyClassPath(
			pluginConfig.thirdPartyClasspath().absolutePathStrings());
	}
	
	public PluginConfiguration getPluginConfiguration()
	{
		return new PluginConfigurationBuilder(this.project)
			.withUseSingleThread(this.chbxUseSingleThread.isSelected())
			.withShowSuppressedWarnings(this.chbxShowSuppressedWarnings.isSelected())
			.withUseCacheFile(this.chbxUseCacheFile.isSelected())
			.withScanScope(Objects.requireNonNullElseGet(
				(ScanScope)this.cbScope.getSelectedItem(),
				ScanScope::getDefaultValue))
			.withProjectRelativeFileExclusionsRaw(this.exclusionPanelManager.getPatterns())
			.withLocations(new TreeSet<>(this.rulefileLocationPanelManager.locationModel().getLocations()))
			.withActiveLocationIds(this.rulefileLocationPanelManager.locationModel().getActiveLocations().stream()
				.map(ConfigurationLocation::getId)
				.collect(Collectors.toCollection(TreeSet::new)))
			.withThirdPartyClasspath(
				ThirdPartyClasspathConfigContainer.createFromUI(
					this.project,
					this.thirdPartyClasspathPanelManager.getThirdPartyClassPath()))
			.withImportSettingFromMaven(this.chbxImportSettingsFromMaven.isSelected())
			.build();
	}
}
