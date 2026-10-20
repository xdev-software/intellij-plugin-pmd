package software.xdev.pmd.ui.config.project.components.thirdpartyclasspath;

import java.awt.Dialog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.project.Project;

import software.xdev.pmd.model.config.thirdpartycplocation.ThirdPartyCPLocation;
import software.xdev.pmd.ui.config.project.components.shared.LocationDialog;


public class TPCPLocationDialog extends LocationDialog<ThirdPartyCPLocation, TPCPLocationPanel>
{
	public TPCPLocationDialog(
		@Nullable final Dialog parent,
		@NotNull final Project project)
	{
		super(parent, project, new TPCPLocationPanel(project));
	}
	
	@Override
	protected ThirdPartyCPLocation getLocationFromPanelAndValidate(final TPCPLocationPanel panel)
	{
		return panel.createLocation();
	}
}
