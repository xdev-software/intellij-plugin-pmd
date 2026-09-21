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
		
		return FormBuilder.createFormBuilder()
			.addComponent(this.createInfoBoxTa("""
				This URL will be used to download maven artifacts instead of https://repo.maven.apache.org/maven2/.
				It takes precedence over a mirror auto-detected from settings.xml.
				Only needed if no usable settings.xml is present or a setup that is not detected properly.
				"""))
			.addLabeledComponent(
				"Artifact download override:",
				this.txtArtifactRepositoryBaseUrlOverride)
			.addComponentFillVertically(new JPanel(), 0)
			.getPanel();
	}
	
	private JTextArea createInfoBoxTa(final String text)
	{
		final JTextArea ta = new JTextArea(text);
		ta.setFont(UIManager.getFont("Label.font"));
		ta.setEditable(false);
		ta.setOpaque(false);
		ta.setWrapStyleWord(true);
		ta.setLineWrap(true);
		return ta;
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
