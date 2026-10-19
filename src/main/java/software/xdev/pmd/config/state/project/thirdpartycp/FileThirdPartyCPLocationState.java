package software.xdev.pmd.config.state.project.thirdpartycp;

import com.intellij.util.xmlb.annotations.Text;


public class FileThirdPartyCPLocationState extends ThirdPartyCPLocationState
{
	@Text
	protected String location;
	
	@SuppressWarnings("unused")
	public FileThirdPartyCPLocationState()
	{
		// for serialisation
	}
	
	public FileThirdPartyCPLocationState(final String id, final String location)
	{
		super(id);
		this.location = location;
	}
	
	public String location()
	{
		return this.location;
	}
}
