package software.xdev.pmd.ui.config.project.components.thirdpartyclasspath;

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
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.JBUI;

import software.xdev.pmd.model.config.thirdpartycplocation.ThirdPartyCPLocation;
import software.xdev.pmd.model.config.thirdpartycplocation.file.absolute.AbsoluteFileThirdPartyCPLocationFactory;
import software.xdev.pmd.model.config.thirdpartycplocation.file.relative.RelativeFileThirdPartyCPLocationFactory;
import software.xdev.pmd.model.config.thirdpartycplocation.maven.MavenId;
import software.xdev.pmd.model.config.thirdpartycplocation.maven.MavenThirdPartyCPLocationFactory;


@SuppressWarnings("checkstyle:MagicNumber")
public class TPCPLocationPanel extends JPanel
{
	enum LocationType
	{
		FILE, MAVEN_ARTIFACT
	}
	
	
	private static final Insets COMPONENT_INSETS = JBUI.insets(4);
	
	private final JRadioButton radioFileLocation = new JRadioButton();
	private final JTextField txtFileLocation = new JTextField(20);
	private final JButton btnBrowse = new JButton(new BrowseAction());
	private final JCheckBox chbxRelativeFile = new JCheckBox();
	
	private final JRadioButton radioMavenArtifact = new JRadioButton();
	private final JTextField txtMavenArtifactGroupId = new JTextField(20);
	private final JTextField txtMavenArtifactArtifactId = new JTextField(20);
	private final JTextField txtMavenArtifactVersion = new JTextField(20);
	
	private final Project project;
	
	public TPCPLocationPanel(final Project project)
	{
		super(new GridBagLayout());
		
		this.project = Objects.requireNonNull(project);
		
		this.initialise();
	}
	
	private void initialise()
	{
		this.chbxRelativeFile.setText("Store relative to project location");
		this.chbxRelativeFile.setToolTipText("The file path should be stored as relative to the project location");
		this.chbxRelativeFile.setSelected(true);
		
		this.radioFileLocation.setText("Use a local file");
		this.radioFileLocation.addActionListener(this.createRadioButtonListener(LocationType.FILE));
		this.radioMavenArtifact.setText("Use a Maven artifact");
		this.radioMavenArtifact.addActionListener(this.createRadioButtonListener(LocationType.MAVEN_ARTIFACT));
		
		final ButtonGroup locationGroup = new ButtonGroup();
		locationGroup.add(this.radioFileLocation);
		locationGroup.add(this.radioMavenArtifact);
		
		this.radioFileLocation.setSelected(true);
		this.enabledLocation(LocationType.FILE);
		
		this.setBorder(JBUI.Borders.empty(8, 8, 4, 8));
		
		int gridY = 0;
		this.add(
			this.radioFileLocation, new GridBagConstraints(
				0, gridY, 3, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
		
		gridY++;
		this.add(
			new JLabel("File:"), new GridBagConstraints(
				0, gridY, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
		this.add(
			this.txtFileLocation, new GridBagConstraints(
				1, gridY, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, COMPONENT_INSETS, 0, 0));
		this.add(
			this.btnBrowse, new GridBagConstraints(
				2, gridY, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
		
		gridY++;
		this.add(
			this.chbxRelativeFile, new GridBagConstraints(
				1, gridY, 2, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
		
		gridY++;
		this.add(
			this.radioMavenArtifact, new GridBagConstraints(
				0, gridY, 3, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
		
		this.addTextFieldLine(++gridY, "GroupId", this.txtMavenArtifactGroupId);
		this.addTextFieldLine(++gridY, "ArtifactId", this.txtMavenArtifactArtifactId);
		this.addTextFieldLine(++gridY, "Version", this.txtMavenArtifactVersion);
		
		this.add(
			Box.createVerticalGlue(), new GridBagConstraints(
				0, ++gridY, 3, 1, 0.0, 1.0,
				GridBagConstraints.WEST, GridBagConstraints.VERTICAL, COMPONENT_INSETS, 0, 0));
	}
	
	private void addTextFieldLine(final int gridY, final String label, final JTextField textField)
	{
		this.add(
			new JLabel(label + ":"), new GridBagConstraints(
				0, gridY, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
		this.add(
			textField, new GridBagConstraints(
				1, gridY, 2, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, COMPONENT_INSETS, 0, 0));
	}
	
	private ActionListener createRadioButtonListener(final LocationType locationType)
	{
		return e -> this.enabledLocation(locationType);
	}
	
	private void enabledLocation(final LocationType locationType)
	{
		final boolean isFile = locationType == LocationType.FILE;
		this.txtFileLocation.setEnabled(isFile);
		this.btnBrowse.setEnabled(isFile);
		this.chbxRelativeFile.setEnabled(isFile);
		
		final boolean isMavenArtifact = locationType == LocationType.MAVEN_ARTIFACT;
		this.txtMavenArtifactGroupId.setEnabled(isMavenArtifact);
		this.txtMavenArtifactArtifactId.setEnabled(isMavenArtifact);
		this.txtMavenArtifactVersion.setEnabled(isMavenArtifact);
	}
	
	public ThirdPartyCPLocation createLocation()
	{
		if(this.txtFileLocation.isEnabled() && this.isNotBlank(this.txtFileLocation.getText()))
		{
			return (this.chbxRelativeFile.isSelected()
				? this.project.getService(RelativeFileThirdPartyCPLocationFactory.class)
				: this.project.getService(AbsoluteFileThirdPartyCPLocationFactory.class))
				.fromUI(this.getAbsoluteFileLocationPath());
		}
		else if(this.txtMavenArtifactGroupId.isEnabled()
			&& this.isNotBlank(this.txtMavenArtifactGroupId.getText())
			&& this.isNotBlank(this.txtMavenArtifactArtifactId.getText())
			&& this.isNotBlank(this.txtMavenArtifactVersion.getText()))
		{
			this.project.getService(MavenThirdPartyCPLocationFactory.class)
				.fromUI(new MavenId(
					this.txtMavenArtifactGroupId.getText(),
					this.txtMavenArtifactArtifactId.getText(),
					this.txtMavenArtifactVersion.getText()));
		}
		
		return null;
	}
	
	private Path getAbsoluteFileLocationPath()
	{
		final String pathStr = this.txtFileLocation.getText();
		Objects.requireNonNull(pathStr);
		if(pathStr.isBlank())
		{
			throw new IllegalArgumentException("Invalid path: " + pathStr);
		}
		
		final Path path = Paths.get(pathStr.trim());
		if(!path.isAbsolute())
		{
			throw new IllegalArgumentException("Non absolute path: " + path);
		}
		
		if(!Files.exists(path))
		{
			throw new IllegalArgumentException("File does not exist: " + path);
		}
		
		return path;
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
				fileLocationPath = Optional.ofNullable(TPCPLocationPanel.this.getAbsoluteFileLocationPath());
			}
			catch(final Exception ex)
			{
				fileLocationPath = Optional.empty();
			}
			final VirtualFile toSelect = fileLocationPath
				.map(LocalFileSystem.getInstance()::findFileByNioFile)
				.orElseGet(() -> ProjectUtil.guessProjectDir(TPCPLocationPanel.this.project));
			
			final VirtualFile chosen = FileChooser.chooseFile(
				chooserDescriptor(),
				TPCPLocationPanel.this,
				TPCPLocationPanel.this.project,
				toSelect);
			if(chosen != null)
			{
				final File newConfigFile = VfsUtilCore.virtualToIoFile(chosen);
				TPCPLocationPanel.this.txtFileLocation.setText(newConfigFile.getAbsolutePath());
			}
		}
		
		private static FileChooserDescriptor chooserDescriptor()
		{
			return new FileChooserDescriptor(true, false, true, true, false, false)
				.withExtensionFilter("jar");
		}
	}
}
