package software.xdev.pmd.ui.config.project;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;

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
	
	private final JLabel scopeDropdownLabel = new JLabel("Scan Scope:");
	private final ComboBox<ScanScope> scopeDropdown = new ComboBox<>(ScanScope.values());
	
	private final LocationTableModel locationModel = new LocationTableModel();
	private final JBTable locationTable = new JBTable(this.locationModel);
	
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
		this.scopeDropdownLabel.setToolTipText("Choose which files should be scanned");
		this.scopeDropdown.setToolTipText("Choose which files should be scanned");
		
		final JPanel configFilePanel = new JPanel(new GridBagLayout());
		configFilePanel.setOpaque(false);
		
		configFilePanel.add(
			this.scopeDropdownLabel, new GridBagConstraints(
				0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
		configFilePanel.add(
			this.scopeDropdown, new GridBagConstraints(
				1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
		configFilePanel.add(
			this.buildRuleFilePanel(), new GridBagConstraints(
				0, 1, 4, 1, 1.0, 1.0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, COMPONENT_INSETS, 0, 0));
		
		return configFilePanel;
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
		this.scopeDropdown.setSelectedItem(pluginConfig.getScanScope());
		this.locationModel.setLocations(new ArrayList<>(pluginConfig.getLocations()));
		this.locationModel.setActiveLocations(pluginConfig.getActiveLocations());
	}
	
	public PluginConfiguration getPluginConfiguration()
	{
		ScanScope scanScope = (ScanScope)this.scopeDropdown.getSelectedItem();
		if(scanScope == null)
		{
			scanScope = ScanScope.getDefaultValue();
		}
		
		// we don't know the scanBeforeCheckin flag at this point
		return PluginConfigurationBuilder.defaultConfiguration(this.project)
			.withScanScope(scanScope)
			.withLocations(new TreeSet<>(this.locationModel.getLocations()))
			.withActiveLocationIds(this.locationModel.getActiveLocations().stream()
				.map(ConfigurationLocation::getId)
				.collect(Collectors.toCollection(TreeSet::new)))
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
