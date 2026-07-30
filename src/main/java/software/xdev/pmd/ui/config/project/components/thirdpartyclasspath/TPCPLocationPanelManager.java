package software.xdev.pmd.ui.config.project.components.thirdpartyclasspath;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import com.intellij.openapi.project.Project;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.ToolbarDecorator;

import software.xdev.pmd.model.config.thirdpartycplocation.ThirdPartyCPLocation;
import software.xdev.pmd.ui.config.project.PMDConfigPanel;
import software.xdev.pmd.ui.config.project.components.shared.LocationPanelManager;


public class TPCPLocationPanelManager
	extends LocationPanelManager<TPCPLocationTableModel, TPCPLocationDialog, ThirdPartyCPLocation>
{
	private static final int TYPE_COL_MIN_WIDTH = 50;
	private static final int TYPE_COL_MAX_WIDTH = 100;
	
	public TPCPLocationPanelManager(final Project project, final PMDConfigPanel pmdConfigPanel)
	{
		super(project, pmdConfigPanel, new TPCPLocationTableModel(), TPCPLocationDialog::new);
	}
	
	@Override
	public JPanel panel()
	{
		this.setColumnWith(this.locationTable, 0, TYPE_COL_MIN_WIDTH, TYPE_COL_MAX_WIDTH, TYPE_COL_MAX_WIDTH);
		this.configureLocationTableDefaults();
		
		final ToolbarDecorator tableDecorator = ToolbarDecorator.createDecorator(this.locationTable)
			.setAddAction(new AddLocationAction())
			.setRemoveAction(new RemoveLocationAction())
			.setMoveDownAction(e -> this.tryMove(1))
			.setMoveUpAction(e -> this.tryMove(-1))
			.setPreferredSize(DECORATOR_DIMENSIONS);
		
		final JPanel container = new JPanel(new BorderLayout());
		container.add(new TitledSeparator("Third-Party Rules"), BorderLayout.NORTH);
		container.add(tableDecorator.createPanel(), BorderLayout.CENTER);
		return container;
	}
	
	private void tryMove(final int direction)
	{
		final int selectedRowIndex = this.locationTable.getSelectedRow();
		this.locationModel.trySwap(selectedRowIndex, selectedRowIndex + direction);
	}
}
