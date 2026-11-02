package software.xdev.pmd.model.config.thirdpartycplocation;

import java.net.URL;
import java.util.Objects;


public abstract class ThirdPartyCPLocation
{
	private final ThirdPartyCPLocationType type;
	private final String id;
	private final URL url;
	
	protected ThirdPartyCPLocation(final ThirdPartyCPLocationType type, final String id, final URL url)
	{
		this.type = Objects.requireNonNull(type);
		this.id = Objects.requireNonNull(id);
		this.url = Objects.requireNonNull(url);
	}
	
	public ThirdPartyCPLocationType type()
	{
		return this.type;
	}
	
	public String id()
	{
		return this.id;
	}
	
	public URL url()
	{
		return this.url;
	}
	
	public abstract String displayLocation();
	
	@Override
	public boolean equals(final Object o)
	{
		if(!(o instanceof final ThirdPartyCPLocation that))
		{
			return false;
		}
		return this.type == that.type;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hashCode(this.type);
	}
}
