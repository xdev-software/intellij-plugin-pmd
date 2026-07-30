package software.xdev.pmd.ui.config.project.components.rulesetlocation;

import static software.xdev.pmd.model.config.rulesetlocation.ConfigurationType.LOCAL_FILE;
import static software.xdev.pmd.model.config.rulesetlocation.ConfigurationType.PROJECT_RELATIVE;
import static software.xdev.pmd.ui.config.project.components.rulesetlocation.RSLocationPanel.LocationType.FILE;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
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

import software.xdev.pmd.model.config.rulesetlocation.ConfigurationLocation;
import software.xdev.pmd.model.config.rulesetlocation.ConfigurationLocationFactory;
import software.xdev.pmd.model.config.rulesetlocation.ConfigurationType;
import software.xdev.pmd.util.io.ProjectFilePaths;


@SuppressWarnings("checkstyle:MagicNumber")
public class RSLocationPanel extends JPanel
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
	
	public RSLocationPanel(final Project project)
	{
		super(new GridBagLayout());
		
		this.project = Objects.requireNonNull(project);
		
		this.initialise();
	}
	
	private void initialise()
	{
		this.relativeFileCheckbox.setText("Store relative to project location");
		this.relativeFileCheckbox.setToolTipText("The file path should be stored as relative to the project location");
		this.relativeFileCheckbox.setSelected(true);
		
		this.fileLocationRadio.setText("Use a local file");
		this.fileLocationRadio.addActionListener(this.createRadioButtonListener(FILE));
		
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
	
	private ActionListener createRadioButtonListener(final LocationType locationType)
	{
		return e -> this.enabledLocation(locationType);
	}
	
	private void enabledLocation(final LocationType locationType)
	{
		final boolean isFile = locationType == FILE;
		
		this.fileLocationField.setEnabled(isFile);
		this.browseButton.setEnabled(isFile);
		this.relativeFileCheckbox.setEnabled(isFile);
	}
	
	private ConfigurationType typeOfFile()
	{
		return this.relativeFileCheckbox.isSelected()
			? PROJECT_RELATIVE
			: LOCAL_FILE;
	}
	
	/**
	 * Get the configuration location entered in the dialogue, or null if no valid location was entered.
	 *
	 * @return the location or null if no valid location entered.
	 */
	public ConfigurationLocation getConfigurationLocation()
	{
		final String newId = UUID.randomUUID().toString();
		
		if(this.fileLocationField.isEnabled() && this.isNotBlank(this.fileLocationField.getText()))
		{
			final ConfigurationType type = this.typeOfFile();
			return this.configurationLocationFactory().create(
				this.project,
				newId,
				type,
				this.project.getService(ProjectFilePaths.class)
					.toUnixPath(this.getFileLocationPath(type).toString()),
				this.descriptionField.getText());
		}
		
		return null;
	}
	
	private Path getFileLocationPath(final ConfigurationType type)
	{
		final String filename = this.trim(this.fileLocationField.getText());
		if(filename == null || filename.isBlank())
		{
			throw new IllegalArgumentException("Invalid path: " + filename);
		}
		
		final Path path = Paths.get(filename);
		if(path.isAbsolute())
		{
			// Handle absolute path
			if(!Files.exists(path))
			{
				throw new IllegalArgumentException("Invalid path: " + path);
			}
			
			if(type != PROJECT_RELATIVE)
			{
				return path;
			}
			
			// Make project relative
			return this.guessProjectNioPath().relativize(path);
		}
		
		// Handle relative path
		// Validate that the file exists
		final Path absolutePath = this.guessProjectNioPath()
			.resolve(path)
			.normalize()
			.toAbsolutePath();
		
		if(!Files.exists(absolutePath))
		{
			throw new IllegalArgumentException("Invalid path: " + absolutePath);
		}
		
		return type == PROJECT_RELATIVE ? path : absolutePath;
	}
	
	private Path guessProjectNioPath()
	{
		return Objects.requireNonNull(
				ProjectUtil.guessProjectDir(this.project),
				"Unable to determine project dir")
			.toNioPath();
	}
	
	private String trim(final String text)
	{
		return text != null ? text.trim() : null;
	}
	
	private ConfigurationLocationFactory configurationLocationFactory()
	{
		return this.project.getService(ConfigurationLocationFactory.class);
	}
	
	private boolean isNotBlank(final String str)
	{
		return str != null && !str.isBlank();
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
			Optional<Path> fileLocationPath;
			try
			{
				fileLocationPath = Optional.ofNullable(RSLocationPanel.this.getFileLocationPath(LOCAL_FILE));
			}
			catch(final Exception ex)
			{
				fileLocationPath = Optional.empty();
			}
			final VirtualFile toSelect = fileLocationPath
				.map(LocalFileSystem.getInstance()::findFileByNioFile)
				.orElseGet(() -> ProjectUtil.guessProjectDir(RSLocationPanel.this.project));
			
			final VirtualFile chosen = FileChooser.chooseFile(
				FileChooserDescriptorFactory.createSingleFileDescriptor("xml"),
				RSLocationPanel.this,
				RSLocationPanel.this.project,
				toSelect);
			if(chosen != null)
			{
				final File newConfigFile = VfsUtilCore.virtualToIoFile(chosen);
				RSLocationPanel.this.fileLocationField.setText(newConfigFile.getAbsolutePath());
			}
		}
	}
}
