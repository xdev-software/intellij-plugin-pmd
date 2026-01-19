package software.xdev.pmd.ui.config.project;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ui.JBUI;

import software.xdev.pmd.model.config.ConfigurationLocation;


/**
 * Allows selection of the location of the file.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class LocationDialog extends DialogWrapper
{
	private static final Insets COMPONENT_INSETS = JBUI.insets(4);
	private static final int WIDTH = 500;
	private static final int HEIGHT = 400;
	
	
	private enum Step
	{
		SELECT(false, true, false),
		ERROR(true, false, false),
		COMPLETE(true, false, true);
		
		private final boolean allowPrevious;
		private final boolean allowNext;
		private final boolean allowCommit;
		
		Step(final boolean allowPrevious, final boolean allowNext, final boolean allowCommit)
		{
			this.allowPrevious = allowPrevious;
			this.allowNext = allowNext;
			this.allowCommit = allowCommit;
		}
		
		private boolean isAllowPrevious()
		{
			return this.allowPrevious;
		}
		
		private boolean isAllowNext()
		{
			return this.allowNext;
		}
		
		private boolean isAllowCommit()
		{
			return this.allowCommit;
		}
	}
	
	
	private final Project project;
	
	private final JPanel centrePanel = new JPanel(new BorderLayout());
	private final LocationPanel locationPanel;
	private final ErrorPanel errorPanel = new ErrorPanel();
	private final CompletePanel completePanel = new CompletePanel();
	
	private final JButton commitButton = new JButton(new NextAction());
	private final JButton previousButton = new JButton(new PreviousAction());
	
	private Step currentStep = Step.SELECT;
	
	private ConfigurationLocation configurationLocation;
	
	public LocationDialog(
		@Nullable final Dialog parent,
		@NotNull final Project project)
	{
		super(project, parent, false, IdeModalityType.IDE);
		
		this.project = project;
		
		this.locationPanel = new LocationPanel(project);
		
		this.initialiseComponents();
	}
	
	@Override
	protected @Nullable JComponent createCenterPanel()
	{
		return this.centrePanel;
	}
	
	private void initialiseComponents()
	{
		this.setTitle("Add Configuration");
		this.setSize(WIDTH, HEIGHT);
		
		this.centrePanel.add(this.panelForCurrentStep(), BorderLayout.CENTER);
		
		this.getRootPane().setDefaultButton(this.commitButton);
		this.moveToStep(Step.SELECT);
		
		this.init();
	}
	
	@Override
	protected JComponent createSouthPanel()
	{
		final JPanel bottomPanel = new JPanel(new GridBagLayout());
		bottomPanel.setBorder(JBUI.Borders.empty(4, 8, 8, 8));
		
		final JButton cancelButton = new JButton(this.getCancelAction());
		bottomPanel.add(
			Box.createHorizontalGlue(), new GridBagConstraints(
				0, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, COMPONENT_INSETS, 0, 0));
		bottomPanel.add(
			cancelButton, new GridBagConstraints(
				1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
		bottomPanel.add(
			this.previousButton, new GridBagConstraints(
				2, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
		bottomPanel.add(
			this.commitButton, new GridBagConstraints(
				3, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
		
		return bottomPanel;
	}
	
	private JPanel panelForCurrentStep()
	{
		return switch(this.currentStep)
		{
			case SELECT -> this.locationPanel;
			case ERROR -> this.errorPanel;
			case COMPLETE -> this.completePanel;
		};
	}
	
	/**
	 * Get the configuration location entered in the dialogue, or null if no valid location was entered.
	 *
	 * @return the location or null if no valid location entered.
	 */
	public ConfigurationLocation getConfigurationLocation()
	{
		return this.configurationLocation;
	}
	
	private void moveToStep(final Step newStep)
	{
		this.centrePanel.remove(this.panelForCurrentStep());
		this.currentStep = newStep;
		
		final String commitText = this.currentStep.isAllowCommit() ? "Finish" : "Next";
		this.commitButton.setText(commitText);
		this.commitButton.setToolTipText(commitText);
		
		this.previousButton.setEnabled(this.currentStep.isAllowPrevious());
		this.commitButton.setEnabled(this.currentStep.isAllowNext() || this.currentStep.isAllowCommit());
		
		this.centrePanel.add(this.panelForCurrentStep(), BorderLayout.CENTER);
		this.centrePanel.validate();
		this.centrePanel.repaint();
	}
	
	private Step continueWithValidate(final ConfigurationLocation location)
	{
		this.configurationLocation = location;
		
		try
		{
			this.configurationLocation.validate();
			return Step.COMPLETE;
		}
		catch(final Exception e)
		{
			this.errorPanel.setError(e);
			return Step.ERROR;
		}
	}
	
	private Step continueWithoutValidate(final ConfigurationLocation location)
	{
		this.configurationLocation = location;
		return Step.COMPLETE;
	}
	
	void onPrevious()
	{
		this.previousButton.setEnabled(false);
		
		switch(this.currentStep)
		{
			case COMPLETE, ERROR:
				this.moveToStep(Step.SELECT);
				return;
			
			default:
				throw new IllegalStateException(
					"Unexpected previous call for step " + this.currentStep);
		}
	}
	
	void onNext()
	{
		this.commitButton.setEnabled(false);
		
		final ConfigurationLocation location;
		switch(this.currentStep)
		{
			case SELECT:
				location = this.locationPanel.getConfigurationLocation();
				if(location == null)
				{
					this.showError("No location has been entered");
					return;
				}
				
				if(location.getDescription() == null || location.getDescription().isEmpty())
				{
					this.showError("No description has been entered");
					return;
				}
				
				if(!this.project.isDefault() || location.canBeResolvedInDefaultProject())
				{
					this.moveToStep(this.continueWithValidate(location));
					return;
				}
				
				this.moveToStep(this.continueWithoutValidate(location));
				return;
			
			case COMPLETE:
				this.close(OK_EXIT_CODE);
				return;
			
			default:
				throw new IllegalStateException(
					"Unexpected next call for step " + this.currentStep);
		}
	}
	
	private void showError(final String formattedMessage)
	{
		Messages.showErrorDialog(
			this.getContentPanel(),
			formattedMessage,
			"Error");
		this.commitButton.setEnabled(true);
	}
	
	private final class NextAction extends AbstractAction
	{
		@Override
		public void actionPerformed(final ActionEvent event)
		{
			LocationDialog.this.onNext();
		}
	}
	
	private class PreviousAction extends AbstractAction
	{
		PreviousAction()
		{
			this.putValue(Action.NAME, "Previous");
			this.putValue(Action.SHORT_DESCRIPTION, "Move to the previous step of the wizard");
			this.putValue(Action.LONG_DESCRIPTION, "Move to the previous step of the wizard");
		}
		
		@Override
		public void actionPerformed(final ActionEvent e)
		{
			LocationDialog.this.onPrevious();
		}
	}
}
