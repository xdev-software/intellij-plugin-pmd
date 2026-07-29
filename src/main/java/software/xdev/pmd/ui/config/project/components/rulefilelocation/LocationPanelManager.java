package software.xdev.pmd.ui.config.project.components.rulefilelocation;

import java.awt.BorderLayout;
import java.awt.Dialog;

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
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.AnActionButtonUpdater;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;

import software.xdev.pmd.model.config.ConfigurationLocation;
import software.xdev.pmd.ui.config.project.PMDConfigPanel;
import software.xdev.pmd.ui.config.project.components.SubPMDConfigPanelManager;


public class LocationPanelManager extends SubPMDConfigPanelManager
{
	private static final int ACTIVE_COL_MIN_WIDTH = 40;
	private static final int ACTIVE_COL_MAX_WIDTH = 55;
	private static final int DESC_COL_MIN_WIDTH = 100;
	private static final int DESC_COL_MAX_WIDTH = 200;
	
	final LocationTableModel locationModel = new LocationTableModel();
	final JBTable locationTable = new JBTable(this.locationModel);
	
	public LocationPanelManager(final Project project, final PMDConfigPanel pmdConfigPanel)
	{
		super(project, pmdConfigPanel);
	}
	
	public JPanel buildPanel()
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
	
	public LocationTableModel locationModel()
	{
		return this.locationModel;
	}
	
	class AddLocationAction implements AnActionButtonRunnable
	{
		@Override
		public void run(final AnActionButton anActionButton)
		{
			final LocationDialog dialogue = new LocationDialog(
				(Dialog)SwingUtilities.getAncestorOfClass(Dialog.class, LocationPanelManager.this.pmdConfigPanel),
				LocationPanelManager.this.project);
			
			if(dialogue.showAndGet())
			{
				final ConfigurationLocation newLocation = dialogue.getConfigurationLocation();
				if(LocationPanelManager.this.locationModel.getLocations().contains(newLocation))
				{
					Messages.showWarningDialog(
						LocationPanelManager.this.project,
						"This location has already been added",
						"Duplicate Location");
				}
				else
				{
					LocationPanelManager.this.locationModel.addLocation(dialogue.getConfigurationLocation());
				}
			}
		}
	}
	
	
	class RemoveLocationAction implements AnActionButtonRunnable
	{
		@Override
		public void run(final AnActionButton anActionButton)
		{
			final int selectedIndex = LocationPanelManager.this.locationTable.getSelectedRow();
			if(selectedIndex == -1)
			{
				return;
			}
			
			LocationPanelManager.this.locationModel.removeLocationAt(selectedIndex);
		}
	}
	
	
	class EnableWhenSelectedAndRemovable implements AnActionButtonUpdater
	{
		@Override
		public boolean isEnabled(@NotNull final AnActionEvent e)
		{
			final int selectedItem = LocationPanelManager.this.locationTable.getSelectedRow();
			return selectedItem >= 0
				&& LocationPanelManager.this.locationModel.getLocationAt(selectedItem).isRemovable();
		}
	}
	
	
	class EnableWhenSelected implements AnActionButtonUpdater
	{
		@Override
		public boolean isEnabled(@NotNull final AnActionEvent e)
		{
			return LocationPanelManager.this.locationTable.getSelectedRow() >= 0;
		}
	}
}
