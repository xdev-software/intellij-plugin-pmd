package software.xdev.pmd.config.state.project.thirdpartycp;

import com.intellij.util.xmlb.annotations.Attribute;


public abstract class ThirdPartyCPLocationState
{
	@Attribute
	protected String id;
	
	protected ThirdPartyCPLocationState()
	{
		// for serialization
	}
	
	protected ThirdPartyCPLocationState(final String id)
	{
		this.id = id;
	}
	
	public String id()
	{
		return this.id;
	}
}
