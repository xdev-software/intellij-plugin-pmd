package software.xdev.pmd.ui.config.project.components.shared;

import java.awt.Dialog;
import java.awt.Dimension;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.table.JBTable;

import software.xdev.pmd.ui.config.project.PMDConfigPanel;


public abstract class LocationPanelManager<
	M extends LocationTableModel<T>,
	D extends LocationDialog<T, ?>,
	T>
{
	protected static final Dimension DECORATOR_DIMENSIONS = new Dimension(300, 50);
	
	protected Project project;
	protected final M locationModel;
	protected final JBTable locationTable;
	protected final Supplier<D> dialogCreator;
	
	protected LocationPanelManager(
		final Project project,
		final PMDConfigPanel pmdConfigPanel,
		final M locationModel,
		final BiFunction<Dialog, Project, D> createDialogFunc)
	{
		this.project = project;
		
		this.locationModel = locationModel;
		this.locationTable = new JBTable(this.locationModel);
		
		this.dialogCreator = () -> createDialogFunc.apply(
			(Dialog)SwingUtilities.getAncestorOfClass(Dialog.class, pmdConfigPanel),
			project);
	}
	
	protected void configureLocationTableDefaults()
	{
		this.locationTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		this.locationTable.setStriped(true);
		this.locationTable.getTableHeader().setReorderingAllowed(false);
	}
	
	public abstract JPanel panel();
	
	protected void setColumnWith(
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
	
	public M locationModel()
	{
		return this.locationModel;
	}
	
	public class AddLocationAction implements AnActionButtonRunnable
	{
		@Override
		public void run(final AnActionButton anActionButton)
		{
			final D dialog = LocationPanelManager.this.dialogCreator.get();
			if(dialog.showAndGet())
			{
				final T newLocation = dialog.getConfigurationLocation();
				if(LocationPanelManager.this.locationModel.getLocations().contains(newLocation))
				{
					Messages.showWarningDialog(
						LocationPanelManager.this.project,
						"This location has already been added",
						"Duplicate Location");
				}
				else
				{
					LocationPanelManager.this.locationModel.addLocation(dialog.getConfigurationLocation());
				}
			}
		}
	}
	
	
	public class RemoveLocationAction implements AnActionButtonRunnable
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
}
