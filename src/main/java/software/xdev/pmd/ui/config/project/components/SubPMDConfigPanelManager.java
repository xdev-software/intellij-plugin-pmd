package software.xdev.pmd.ui.config.project.components;

import java.awt.Dimension;

import com.intellij.openapi.project.Project;

import software.xdev.pmd.ui.config.project.PMDConfigPanel;


public abstract class SubPMDConfigPanelManager
{
	protected static final Dimension DECORATOR_DIMENSIONS = new Dimension(300, 50);
	
	protected final Project project;
	protected final PMDConfigPanel pmdConfigPanel;
	
	public SubPMDConfigPanelManager(final Project project, final PMDConfigPanel pmdConfigPanel)
	{
		this.project = project;
		this.pmdConfigPanel = pmdConfigPanel;
	}
}
