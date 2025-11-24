package software.xdev.pmd.ui.config.module;

import static com.intellij.util.ui.JBUI.emptyInsets;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.util.ui.JBUI;

import software.xdev.pmd.model.config.ConfigurationLocation;


/**
 * Provides module level configuration UI.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class PMDModuleConfigPanel extends JPanel
{
	private static final Insets ISOLATED_COMPONENT_INSETS = JBUI.insets(8);
	private final JRadioButton useProjectConfigurationRadio = new JRadioButton();
	private final JRadioButton useModuleConfigurationRadio = new JRadioButton();
	private final JRadioButton excludeRadio = new JRadioButton();
	private final ComboBox<ConfigurationLocation> configurationFilesCombo = new ComboBox<>();
	private final DefaultComboBoxModel<ConfigurationLocation> configurationFilesModel = new DefaultComboBoxModel<>();
	private final JLabel configurationFilesLabel = new JLabel();
	
	private List<ConfigurationLocation> configurationLocations = new ArrayList<>();
	private List<ConfigurationLocation> activeLocations = List.of();
	private boolean excluded;
	
	/**
	 * Create a new panel.
	 */
	public PMDModuleConfigPanel()
	{
		super(new BorderLayout());
		
		this.init();
	}
	
	private void init()
	{
		this.add(this.buildConfigurationPanel(), BorderLayout.CENTER);
	}
	
	private JPanel buildConfigurationPanel()
	{
		final JPanel configPanel = new JPanel(new GridBagLayout());
		
		final JLabel informationLabel = new JLabel(
			"You can configure available files in the project configuration",
			AllIcons.General.Information, SwingConstants.LEFT);
		configPanel.add(
			informationLabel, new GridBagConstraints(
				0, 0, 2, 1, 1.0, 0.0,
				GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, JBUI.insets(16, 8), 0, 0));
		
		this.useProjectConfigurationRadio.setText("Use project configuration");
		this.useProjectConfigurationRadio.setToolTipText(
			"If selected then the project-level settings will be used for this module");
		this.useProjectConfigurationRadio.addActionListener(new RadioListener());
		
		this.useModuleConfigurationRadio.setText("Use a custom configuration");
		this.useModuleConfigurationRadio.setToolTipText(
			"If selected then the selected rules will be used for this module");
		this.useModuleConfigurationRadio.addActionListener(new RadioListener());
		
		this.excludeRadio.setText("Exclude this module from all checks");
		this.excludeRadio.setToolTipText("If selected then no checks will be run against this module");
		this.excludeRadio.addActionListener(new RadioListener());
		
		final ButtonGroup radioGroup = new ButtonGroup();
		radioGroup.add(this.useProjectConfigurationRadio);
		radioGroup.add(this.useModuleConfigurationRadio);
		radioGroup.add(this.excludeRadio);
		
		configPanel.add(
			this.useProjectConfigurationRadio, new GridBagConstraints(
				0, 1, 2, 1, 1.0, 0.0,
				GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, ISOLATED_COMPONENT_INSETS, 0, 0));
		configPanel.add(
			this.useModuleConfigurationRadio, new GridBagConstraints(
				0, 2, 2, 1, 1.0, 0.0,
				GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, ISOLATED_COMPONENT_INSETS, 0, 0));
		
		this.configurationFilesLabel.setText("Rules:");
		configPanel.add(
			this.configurationFilesLabel, new GridBagConstraints(
				0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, JBUI.insets(8, 32, 8, 8), 0, 0));
		
		this.configurationFilesCombo.setToolTipText("The selected Checkstyle rules will be used for this module");
		this.configurationFilesCombo.setModel(this.configurationFilesModel);
		configPanel.add(
			this.configurationFilesCombo, new GridBagConstraints(
				1, 3, 1, 1, 1.0, 0.0,
				GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, ISOLATED_COMPONENT_INSETS, 0, 0));
		
		configPanel.add(
			this.excludeRadio, new GridBagConstraints(
				0, 4, 2, 1, 1.0, 0.0,
				GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, ISOLATED_COMPONENT_INSETS, 0, 0));
		
		configPanel.add(
			Box.createGlue(), new GridBagConstraints(
				0, 5, 2, 1, 1.0, 1.0,
				GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, emptyInsets(), 0, 0));
		
		this.useProjectConfigurationRadio.setSelected(true);
		this.configurationFilesLabel.setEnabled(false);
		this.configurationFilesCombo.setEnabled(false);
		
		return configPanel;
	}
	
	private List<ConfigurationLocation> getConfigurationLocations()
	{
		final List<ConfigurationLocation> locations = new ArrayList<>();
		
		for(int i = 0; i < this.configurationFilesModel.getSize(); ++i)
		{
			locations.add(this.configurationFilesModel.getElementAt(i));
		}
		
		return Collections.unmodifiableList(locations);
	}
	
	public void setConfigurationLocations(final List<ConfigurationLocation> locations)
	{
		this.configurationLocations = Objects.requireNonNullElseGet(locations, ArrayList::new);
		
		this.configurationFilesModel.removeAllElements();
		
		if(locations != null && !locations.isEmpty())
		{
			locations.forEach(this.configurationFilesModel::addElement);
			this.configurationFilesModel.setSelectedItem(locations.getFirst());
		}
		
		this.useModuleConfigurationRadio.setEnabled(locations != null && !locations.isEmpty());
	}
	
	/**
	 * Set the configuration to use, or null to use the project configuration.
	 *
	 * @param activeLocations the configuration, or null to use the project configuration.
	 */
	public void setActiveLocations(final List<ConfigurationLocation> activeLocations)
	{
		this.activeLocations = Objects.requireNonNullElseGet(activeLocations, List::of);
		
		if(!activeLocations.isEmpty())
		{
			this.configurationFilesCombo.setSelectedItem(activeLocations.getFirst());
		}
		else if(this.configurationFilesModel.getSize() > 0)
		{
			this.configurationFilesCombo.setSelectedItem(this.configurationFilesModel.getElementAt(0));
		}
		
		if(!activeLocations.isEmpty())
		{
			this.useModuleConfigurationRadio.setSelected(true);
		}
		else if(!this.excluded)
		{
			this.useProjectConfigurationRadio.setSelected(true);
		}
		
		new RadioListener().actionPerformed(null);
	}
	
	/**
	 * Get the configuration to use, or null to use the project configuration.
	 *
	 * @return the configuration, or null to use the project configuration.
	 */
	public ConfigurationLocation getActiveLocation()
	{
		if(this.useProjectConfigurationRadio.isSelected() || this.excludeRadio.isSelected())
		{
			return null;
		}
		
		return (ConfigurationLocation)this.configurationFilesModel.getSelectedItem();
	}
	
	public void setExcluded(final boolean excluded)
	{
		this.excluded = excluded;
		
		if(excluded)
		{
			this.excludeRadio.setSelected(true);
		}
	}
	
	public boolean isExcluded()
	{
		return this.excludeRadio.isSelected();
	}
	
	/**
	 * Have the contents been modified since being set?
	 *
	 * @return true if modified.
	 */
	public boolean isModified()
	{
		return !this.activeLocations.contains(this.getActiveLocation())
			|| !Objects.equals(this.configurationLocations, this.getConfigurationLocations())
			|| this.excluded != this.isExcluded();
	}
	
	/**
	 * Listener to update UI based on radio button selections.
	 */
	private final class RadioListener implements ActionListener
	{
		@Override
		public void actionPerformed(final ActionEvent e)
		{
			final boolean showModuleConfig = PMDModuleConfigPanel.this.useModuleConfigurationRadio.isSelected();
			
			PMDModuleConfigPanel.this.configurationFilesLabel.setEnabled(showModuleConfig);
			PMDModuleConfigPanel.this.configurationFilesCombo.setEnabled(showModuleConfig);
		}
	}
}
