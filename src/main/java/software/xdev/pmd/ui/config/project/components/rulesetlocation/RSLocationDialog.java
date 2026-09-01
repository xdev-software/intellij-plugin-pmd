package software.xdev.pmd.ui.config.project.components.rulesetlocation;

import java.awt.Dialog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.project.Project;

import software.xdev.pmd.analysis.ProjectRulesetClasspathManager;
import software.xdev.pmd.model.config.rulesetlocation.ConfigurationLocation;
import software.xdev.pmd.ui.config.project.components.shared.LocationDialog;


@SuppressWarnings("checkstyle:MagicNumber")
public class RSLocationDialog
	extends LocationDialog<ConfigurationLocation, RSLocationPanel>
{
	public RSLocationDialog(
		@Nullable final Dialog parent,
		@NotNull final Project project)
	{
		super(parent, project, new RSLocationPanel(project));
		this.setErrorPanel(new RSErrorPanel());
	}
	
	@Override
	protected ConfigurationLocation getLocationFromPanelAndValidate(final RSLocationPanel panel) throws Exception
	{
		final ConfigurationLocation location;
		try
		{
			location = this.locationPanel.getConfigurationLocation();
		}
		catch(final Exception ex)
		{
			this.showError("Failed to get configuration: " + ex.getMessage());
			this.logger.debug("Failed to get configuration", ex);
			return null;
		}
		if(location == null)
		{
			this.showError("No location has been entered");
			return null;
		}
		
		if(location.getDescription() == null || location.getDescription().isEmpty())
		{
			this.showError("No description has been entered");
			return null;
		}
		
		location.validate(this.project.getService(ProjectRulesetClasspathManager.class).getClassLoader());
		
		return location;
	}
}
