package software.xdev.pmd.ui.config.project.components.rulesetlocation;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.jetbrains.annotations.NotNull;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AnActionButtonUpdater;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.util.ui.JBUI;

import software.xdev.pmd.model.config.rulesetlocation.ConfigurationLocation;
import software.xdev.pmd.ui.config.project.PMDConfigPanel;
import software.xdev.pmd.ui.config.project.components.shared.LocationPanelManager;


public class RSLocationPanelManager
	extends LocationPanelManager<RSLocationTableModel, RSLocationDialog, ConfigurationLocation>
{
	private static final int ACTIVE_COL_MIN_WIDTH = 40;
	private static final int ACTIVE_COL_MAX_WIDTH = 55;
	private static final int DESC_COL_MIN_WIDTH = 100;
	private static final int DESC_COL_MAX_WIDTH = 200;
	
	public RSLocationPanelManager(final Project project, final PMDConfigPanel pmdConfigPanel)
	{
		super(project, pmdConfigPanel, new RSLocationTableModel(), RSLocationDialog::new);
	}
	
	@Override
	public JPanel panel()
	{
		this.setColumnWith(this.locationTable, 0, ACTIVE_COL_MIN_WIDTH, ACTIVE_COL_MAX_WIDTH, ACTIVE_COL_MAX_WIDTH);
		this.setColumnWith(this.locationTable, 1, DESC_COL_MIN_WIDTH, DESC_COL_MAX_WIDTH, DESC_COL_MAX_WIDTH);
		this.configureLocationTableDefaults();
		
		final ToolbarDecorator tableDecorator = ToolbarDecorator.createDecorator(this.locationTable)
			.setAddAction(new AddLocationAction())
			.setRemoveAction(new RemoveLocationAction())
			.setEditActionUpdater(new EnableWhenSelected())
			.setRemoveActionUpdater(new EnableWhenSelectedAndRemovable())
			.setPreferredSize(DECORATOR_DIMENSIONS);
		
		final JPanel container = new JPanel(new BorderLayout());
		container.add(new TitledSeparator("Configuration File"), BorderLayout.NORTH);
		container.add(tableDecorator.createPanel(), BorderLayout.CENTER);
		final JLabel infoLabel = new JLabel(
			"The active rules may be overridden or deactivated by module settings",
			AllIcons.General.Information, SwingConstants.LEFT);
		infoLabel.setBorder(JBUI.Borders.empty(8, 0, 4, 0));
		container.add(infoLabel, BorderLayout.SOUTH);
		return container;
	}
	
	class EnableWhenSelectedAndRemovable implements AnActionButtonUpdater
	{
		@Override
		public boolean isEnabled(@NotNull final AnActionEvent e)
		{
			final int selectedItem = RSLocationPanelManager.this.locationTable.getSelectedRow();
			return selectedItem >= 0
				&& RSLocationPanelManager.this.locationModel.getLocationAt(selectedItem).isRemovable();
		}
	}
	
	class EnableWhenSelected implements AnActionButtonUpdater
	{
		@Override
		public boolean isEnabled(@NotNull final AnActionEvent e)
		{
			return RSLocationPanelManager.this.locationTable.getSelectedRow() >= 0;
		}
	}
}
