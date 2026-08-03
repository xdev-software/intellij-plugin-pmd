package software.xdev.pmd.ui.config.project;

import java.awt.BorderLayout;
import java.awt.Dialog;
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
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;

import org.jetbrains.annotations.NotNull;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.AnActionButtonUpdater;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;

import software.xdev.pmd.config.PatternContainer;
import software.xdev.pmd.config.PluginConfiguration;
import software.xdev.pmd.config.PluginConfigurationBuilder;
import software.xdev.pmd.model.config.ConfigurationLocation;
import software.xdev.pmd.model.scope.ScanScope;


/**
 * Provides a configuration panel (dialog) for project-level configuration.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class PMDConfigPanel extends JPanel
{
	private static final Insets COMPONENT_INSETS = JBUI.insets(4);
	private static final int ACTIVE_COL_MIN_WIDTH = 40;
	private static final int ACTIVE_COL_MAX_WIDTH = 55;
	private static final int DESC_COL_MIN_WIDTH = 100;
	private static final int DESC_COL_MAX_WIDTH = 200;
	private static final Dimension DECORATOR_DIMENSIONS = new Dimension(300, 50);
	
	private final JLabel lblScopeDropdown = new JLabel("Scan Scope:");
	private final ComboBox<ScanScope> cbScope = new ComboBox<>(ScanScope.values());
	private final JBCheckBox chbxUseSingleThread = new JBCheckBox("Use single thread");
	private final JBCheckBox chbxShowSuppressedWarnings = new JBCheckBox("Show suppressed warnings");
	private final JBCheckBox chbxUseCacheFile = new JBCheckBox("Use cache file");
	private final JBCheckBox chbxImportSettingsFromMaven = new JBCheckBox("Import settings from Maven");
	
	private final LocationTableModel locationModel = new LocationTableModel();
	private final JBTable locationTable = new JBTable(this.locationModel);
	
	private final FileMaskPanelContainer exclusionPanelContainer = new FileMaskPanelContainer(
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
	
	private final Project project;
	
	public PMDConfigPanel(@NotNull final Project project)
	{
		super(new BorderLayout());
		
		this.project = project;
		
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
		
		configFilePanel.add(
			this.buildRuleFilePanel(),
			this.createFullWidthGridBagConstraints(3, 1.0));
		
		configFilePanel.add(
			this.exclusionPanelContainer.getPanel(),
			this.createFullWidthGridBagConstraints(4, 0.1));
		
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
	
	private JPanel buildRuleFilePanel()
	{
		this.setColumnWith(this.locationTable, 0, ACTIVE_COL_MIN_WIDTH, ACTIVE_COL_MAX_WIDTH, ACTIVE_COL_MAX_WIDTH);
		this.setColumnWith(this.locationTable, 1, DESC_COL_MIN_WIDTH, DESC_COL_MAX_WIDTH, DESC_COL_MAX_WIDTH);
		this.locationTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		this.locationTable.setStriped(true);
		this.locationTable.getTableHeader().setReorderingAllowed(false);
		
		final ToolbarDecorator tableDecorator = ToolbarDecorator.createDecorator(this.locationTable);
		tableDecorator.setAddAction(new AddLocationAction());
		tableDecorator.setRemoveAction(new RemoveLocationAction());
		tableDecorator.setEditActionUpdater(new EnableWhenSelected());
		tableDecorator.setRemoveActionUpdater(new EnableWhenSelectedAndRemovable());
		tableDecorator.setPreferredSize(DECORATOR_DIMENSIONS);
		
		final JPanel container = new JPanel(new BorderLayout());
		container.add(new TitledSeparator("Configuration File"), BorderLayout.NORTH);
		container.add(tableDecorator.createPanel(), BorderLayout.CENTER);
		final JLabel infoLabel = new JLabel(
			"The active rules file may be overridden, or deactivated, by module settings.",
			AllIcons.General.Information, SwingConstants.LEFT);
		infoLabel.setBorder(JBUI.Borders.empty(8, 0, 4, 0));
		container.add(infoLabel, BorderLayout.SOUTH);
		return container;
	}
	
	private void setColumnWith(
		final JTable table,
		final int columnIndex,
		final int minSize,
		final int preferredSize,
		final Integer maxSize)
	{
		final TableColumn column = table.getColumnModel().getColumn(columnIndex);
		column.setMinWidth(minSize);
		column.setWidth(preferredSize);
		column.setPreferredWidth(preferredSize);
		if(maxSize != null)
		{
			column.setMaxWidth(maxSize);
		}
	}
	
	public void showPluginConfiguration(@NotNull final PluginConfiguration pluginConfig)
	{
		this.cbScope.setSelectedItem(pluginConfig.scanScope());
		this.chbxUseSingleThread.setSelected(pluginConfig.useSingleThread());
		this.chbxShowSuppressedWarnings.setSelected(pluginConfig.showSuppressedWarnings());
		this.chbxUseCacheFile.setSelected(pluginConfig.useCacheFile());
		this.chbxImportSettingsFromMaven.setSelected(pluginConfig.importSettingsFromMaven());
		this.locationModel.setLocations(new ArrayList<>(pluginConfig.locations()));
		this.locationModel.setActiveLocations(pluginConfig.getActiveLocations());
		this.exclusionPanelContainer.update(pluginConfig.projectRelativeFileExclusions().stream()
			.map(PatternContainer::patternString)
			.collect(Collectors.toCollection(TreeSet::new)));
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
			.withProjectRelativeFileExclusionsRaw(this.exclusionPanelContainer.getPatterns())
			.withLocations(new TreeSet<>(this.locationModel.getLocations()))
			.withActiveLocationIds(this.locationModel.getActiveLocations().stream()
				.map(ConfigurationLocation::getId)
				.collect(Collectors.toCollection(TreeSet::new)))
			.withImportSettingFromMaven(this.chbxImportSettingsFromMaven.isSelected())
			.build();
	}
	
	/**
	 * Process the addition of a configuration location.
	 */
	private final class AddLocationAction implements AnActionButtonRunnable
	{
		@Override
		public void run(final AnActionButton anActionButton)
		{
			final LocationDialog dialogue = new LocationDialog(
				PMDConfigPanel.this.parentDialogue(),
				PMDConfigPanel.this.project);
			
			if(dialogue.showAndGet())
			{
				final ConfigurationLocation newLocation = dialogue.getConfigurationLocation();
				if(PMDConfigPanel.this.locationModel.getLocations().contains(newLocation))
				{
					Messages.showWarningDialog(
						PMDConfigPanel.this.project,
						"This location has already been added",
						"Duplicate Location");
				}
				else
				{
					PMDConfigPanel.this.locationModel.addLocation(dialogue.getConfigurationLocation());
				}
			}
		}
	}
	
	private Dialog parentDialogue()
	{
		return (Dialog)SwingUtilities.getAncestorOfClass(Dialog.class, PMDConfigPanel.this);
	}
	
	/**
	 * Process the removal of a configuration location.
	 */
	private final class RemoveLocationAction implements AnActionButtonRunnable
	{
		@Override
		public void run(final AnActionButton anActionButton)
		{
			final int selectedIndex = PMDConfigPanel.this.locationTable.getSelectedRow();
			if(selectedIndex == -1)
			{
				return;
			}
			
			PMDConfigPanel.this.locationModel.removeLocationAt(selectedIndex);
		}
	}
	
	
	private final class EnableWhenSelectedAndRemovable implements AnActionButtonUpdater
	{
		@Override
		public boolean isEnabled(@NotNull final AnActionEvent e)
		{
			final int selectedItem = PMDConfigPanel.this.locationTable.getSelectedRow();
			return selectedItem >= 0 && PMDConfigPanel.this.locationModel.getLocationAt(selectedItem)
				.isRemovable();
		}
	}
	
	
	private final class EnableWhenSelected implements AnActionButtonUpdater
	{
		@Override
		public boolean isEnabled(@NotNull final AnActionEvent e)
		{
			final int selectedItem = PMDConfigPanel.this.locationTable.getSelectedRow();
			return selectedItem >= 0;
		}
	}
}
