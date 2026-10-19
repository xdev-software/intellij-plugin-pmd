package software.xdev.pmd.ui.config.project.components.rulesetlocation;

import software.xdev.pmd.ui.config.project.components.shared.ErrorPanel;


public class RSErrorPanel extends ErrorPanel
{
	@Override
	protected Throwable extractCause(final Throwable t)
	{
		if(t.getCause() != null
			&& t.getCause() != t
			&& !t.getClass().getPackage().getName().startsWith("net.sourceforge.pmd"))
		{
			return this.extractCause(t.getCause());
		}
		return t;
	}
}
