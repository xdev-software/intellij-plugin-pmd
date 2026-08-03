package software.xdev.pmd.config.state.project.thirdpartycp;

import com.intellij.util.xmlb.annotations.Attribute;


public class MavenThirdPartyCPLocationState extends ThirdPartyCPLocationState
{
	@Attribute
	protected String groupId;
	@Attribute
	protected String artifactId;
	@Attribute
	protected String version;
	
	@SuppressWarnings("unused")
	public MavenThirdPartyCPLocationState()
	{
		// for serialization
	}
	
	
	public MavenThirdPartyCPLocationState(
		final String id,
		final String groupId,
		final String artifactId,
		final String version)
	{
		super(id);
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
	}
	
	public String groupId()
	{
		return this.groupId;
	}
	
	public String artifactId()
	{
		return this.artifactId;
	}
	
	public String version()
	{
		return this.version;
	}
}
