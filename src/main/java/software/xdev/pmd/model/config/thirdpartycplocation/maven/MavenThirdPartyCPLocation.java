package software.xdev.pmd.model.config.thirdpartycplocation.maven;

import java.net.URL;
import java.util.Objects;

import software.xdev.pmd.maven.MavenId;
import software.xdev.pmd.model.config.thirdpartycplocation.ThirdPartyCPLocation;
import software.xdev.pmd.model.config.thirdpartycplocation.ThirdPartyCPLocationType;


public class MavenThirdPartyCPLocation extends ThirdPartyCPLocation
{
	private final MavenId mavenId;
	
	public MavenThirdPartyCPLocation(
		final String id,
		final URL url,
		final MavenId mavenId)
	{
		super(ThirdPartyCPLocationType.MAVEN_ARTIFACT, id, url);
		this.mavenId = mavenId;
	}
	
	public MavenId mavenId()
	{
		return this.mavenId;
	}
	
	@Override
	public String displayLocation()
	{
		return this.mavenId.toString();
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(o == null || this.getClass() != o.getClass())
		{
			return false;
		}
		if(!super.equals(o))
		{
			return false;
		}
		final MavenThirdPartyCPLocation that = (MavenThirdPartyCPLocation)o;
		return Objects.equals(this.mavenId, that.mavenId);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), this.mavenId);
	}
}
