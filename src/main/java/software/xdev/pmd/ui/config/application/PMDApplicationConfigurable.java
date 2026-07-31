package software.xdev.pmd.ui.config.application;

import java.util.Objects;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.util.ui.FormBuilder;

import software.xdev.pmd.config.state.application.ApplicationConfigurationState;


public class PMDApplicationConfigurable implements Configurable
{
	private final ApplicationConfigurationState applicationConfigurationState;
	private JTextField txtArtifactRepositoryBaseUrlOverride;
	
	public PMDApplicationConfigurable()
	{
		this(ApplicationManager.getApplication().getService(ApplicationConfigurationState.class));
	}
	
	PMDApplicationConfigurable(@NotNull final ApplicationConfigurationState applicationConfigurationState)
	{
		this.applicationConfigurationState = applicationConfigurationState;
	}
	
	@Override
	public String getDisplayName()
	{
		return "PMD Global Settings";
	}
	
	@Override
	public JComponent createComponent()
	{
		this.txtArtifactRepositoryBaseUrlOverride = new JTextField();
		
		this.reset();
		
		final JTextArea taDesc = new JTextArea("""
			This URL will be used to download maven artifact versions instead of https://repo.maven.apache.org/maven2/.
			It takes precedence over a mirror auto-detected from Mavens settings.xml.
			Only needed if no usable settings.xml is present or a setup that is not detected properly.
			""");
		taDesc.setFont(UIManager.getFont("Label.font"));
		taDesc.setEditable(false);
		taDesc.setOpaque(false);
		taDesc.setWrapStyleWord(true);
		taDesc.setLineWrap(true);
		
		return FormBuilder.createFormBuilder()
			.addComponent(taDesc)
			.addLabeledComponent(
				"Artifact download mirror override:",
				this.txtArtifactRepositoryBaseUrlOverride)
			.addComponentFillVertically(new JPanel(), 0)
			.getPanel();
	}
	
	@Override
	public boolean isModified()
	{
		return !Objects.equals(
			normalise(this.txtArtifactRepositoryBaseUrlOverride.getText()),
			this.applicationConfigurationState.getArtifactRepositoryBaseUrlOverride());
	}
	
	@Override
	public void apply()
	{
		this.applicationConfigurationState.setArtifactRepositoryBaseUrlOverride(
			normalise(this.txtArtifactRepositoryBaseUrlOverride.getText()));
	}
	
	@Override
	public void reset()
	{
		this.txtArtifactRepositoryBaseUrlOverride.setText(
			Objects.requireNonNullElse(this.applicationConfigurationState.getArtifactRepositoryBaseUrlOverride(), ""));
	}
	
	@Nullable
	private static String normalise(@Nullable final String value)
	{
		if(value == null || value.isBlank())
		{
			return null;
		}
		return value.trim();
	}
}
