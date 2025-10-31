package software.xdev.pmd.ui.config.project;

import static software.xdev.pmd.model.config.ConfigurationType.LOCAL_FILE;
import static software.xdev.pmd.model.config.ConfigurationType.PROJECT_RELATIVE;
import static software.xdev.pmd.ui.config.project.LocationPanel.LocationType.FILE;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Paths;
import java.util.UUID;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.JBUI;

import software.xdev.pmd.model.config.ConfigurationLocation;
import software.xdev.pmd.model.config.ConfigurationLocationFactory;
import software.xdev.pmd.model.config.ConfigurationType;


@SuppressWarnings("checkstyle:MagicNumber")
public class LocationPanel extends JPanel
{
	enum LocationType
	{
		FILE
	}
	
	
	private static final Insets COMPONENT_INSETS = JBUI.insets(4);
	
	private final JButton browseButton = new JButton(new BrowseAction());
	private final JTextField fileLocationField = new JTextField(20);
	private final JRadioButton fileLocationRadio = new JRadioButton();
	private final JTextField descriptionField = new JTextField();
	private final JCheckBox relativeFileCheckbox = new JCheckBox();
	
	private final Project project;
	
	public LocationPanel(final Project project)
	{
		super(new GridBagLayout());
		
		if(project == null)
		{
			throw new IllegalArgumentException("Project may not be null");
		}
		this.project = project;
		
		this.initialise();
	}
	
	private void initialise()
	{
		this.relativeFileCheckbox.setText("Store relative to project location");
		this.relativeFileCheckbox.setToolTipText("The file path should be stored as relative to the project location");
		
		this.fileLocationRadio.setText("Use a local file");
		this.fileLocationRadio.addActionListener(new RadioButtonActionListener());
		
		final ButtonGroup locationGroup = new ButtonGroup();
		locationGroup.add(this.fileLocationRadio);
		
		this.fileLocationRadio.setSelected(true);
		this.enabledLocation(FILE);
		
		final JLabel descriptionLabel = new JLabel("Description:");
		this.descriptionField.setToolTipText("A description of this configuration file");
		
		final JLabel fileLocationLabel = new JLabel("File:");
		
		this.setBorder(JBUI.Borders.empty(8, 8, 4, 8));
		
		this.add(
			descriptionLabel, new GridBagConstraints(
				0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
		this.add(
			this.descriptionField, new GridBagConstraints(
				1, 0, 2, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, COMPONENT_INSETS, 0, 0));
		
		this.add(
			this.fileLocationRadio, new GridBagConstraints(
				0, 1, 3, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
		
		this.add(
			fileLocationLabel, new GridBagConstraints(
				0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
		this.add(
			this.fileLocationField, new GridBagConstraints(
				1, 2, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, COMPONENT_INSETS, 0, 0));
		this.add(
			this.browseButton, new GridBagConstraints(
				2, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
		
		this.add(
			this.relativeFileCheckbox, new GridBagConstraints(
				1, 3, 2, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
		
		this.add(
			Box.createVerticalGlue(), new GridBagConstraints(
				0, 10, 3, 1, 0.0, 1.0,
				GridBagConstraints.WEST, GridBagConstraints.VERTICAL, COMPONENT_INSETS, 0, 0));
	}
	
	private void enabledLocation(final LocationType locationType)
	{
		this.fileLocationField.setEnabled(locationType == FILE);
		this.browseButton.setEnabled(locationType == FILE);
		this.relativeFileCheckbox.setEnabled(locationType == FILE);
	}
	
	private ConfigurationType typeOfFile()
	{
		if(this.relativeFileCheckbox.isSelected())
		{
			return PROJECT_RELATIVE;
		}
		return LOCAL_FILE;
	}
	
	/**
	 * Get the configuration location entered in the dialogue, or null if no valid location was entered.
	 *
	 * @return the location or null if no valid location entered.
	 */
	public ConfigurationLocation getConfigurationLocation()
	{
		final String newId = UUID.randomUUID().toString();
		
		if(this.fileLocationField.isEnabled())
		{
			if(this.isNotBlank(this.fileLocation()))
			{
				return this.configurationLocationFactory().create(
					this.project,
					newId,
					this.typeOfFile(),
					this.fileLocation(),
					this.descriptionField.getText());
			}
		}
		
		return null;
	}
	
	private String fileLocation()
	{
		final String filename = this.trim(this.fileLocationField.getText());
		
		if(new File(filename).exists())
		{
			return filename;
		}
		
		final File projectRelativePath = this.projectRelativeFileOf(filename);
		if(projectRelativePath.exists())
		{
			return projectRelativePath.getAbsolutePath();
		}
		
		return filename;
	}
	
	private File projectRelativeFileOf(final String filename)
	{
		return Paths.get(new File(this.project.getBasePath(), filename).getAbsolutePath())
			.normalize()
			.toAbsolutePath()
			.toFile();
	}
	
	private String trim(final String text)
	{
		if(text != null)
		{
			return text.trim();
		}
		return null;
	}
	
	private ConfigurationLocationFactory configurationLocationFactory()
	{
		return this.project.getService(ConfigurationLocationFactory.class);
	}
	
	private boolean isNotBlank(final String str)
	{
		return str != null && !str.isBlank();
	}
	
	/**
	 * Set the configuration location.
	 *
	 * @param configurationLocation the location.
	 */
	public void setConfigurationLocation(final ConfigurationLocation configurationLocation)
	{
		this.relativeFileCheckbox.setSelected(false);
		
		if(configurationLocation == null)
		{
			this.fileLocationRadio.setEnabled(true);
			this.fileLocationField.setText(null);
		}
		else if(configurationLocation.getType() == LOCAL_FILE
			|| configurationLocation.getType() == PROJECT_RELATIVE)
		{
			this.fileLocationRadio.setEnabled(true);
			this.fileLocationField.setText(configurationLocation.getLocation());
			this.relativeFileCheckbox.setSelected(configurationLocation.getType() == PROJECT_RELATIVE);
		}
		throw new IllegalArgumentException("Unsupported configuration type: " + configurationLocation.getType());
	}
	
	private final class BrowseAction extends AbstractAction
	{
		BrowseAction()
		{
			this.putValue(Action.NAME, "Browse");
			this.putValue(
				Action.SHORT_DESCRIPTION,
				"Browse the file-system for a configuration file");
			this.putValue(
				Action.LONG_DESCRIPTION,
				"Browse the file-system for a configuration file");
		}
		
		@Override
		public void actionPerformed(final ActionEvent e)
		{
			final String configFilePath = LocationPanel.this.fileLocation();
			final VirtualFile toSelect = (configFilePath != null && !configFilePath.isBlank())
				? LocalFileSystem.getInstance().findFileByPath(configFilePath)
				: ProjectUtil.guessProjectDir(LocationPanel.this.project);
			
			final VirtualFile chosen = FileChooser.chooseFile(
				FileChooserDescriptorFactory.createSingleFileDescriptor("xml"),
				LocationPanel.this,
				LocationPanel.this.project,
				toSelect);
			if(chosen != null)
			{
				final File newConfigFile = VfsUtilCore.virtualToIoFile(chosen);
				LocationPanel.this.fileLocationField.setText(newConfigFile.getAbsolutePath());
			}
		}
	}
	
	
	/**
	 * Handles radio button selections.
	 */
	private final class RadioButtonActionListener implements ActionListener
	{
		@Override
		public void actionPerformed(final ActionEvent e)
		{
			if(LocationPanel.this.fileLocationRadio.isSelected())
			{
				LocationPanel.this.enabledLocation(FILE);
			}
			throw new IllegalStateException("Unknown radio button state");
		}
	}
}
