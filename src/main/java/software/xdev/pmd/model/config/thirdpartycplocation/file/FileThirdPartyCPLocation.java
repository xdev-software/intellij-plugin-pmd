package software.xdev.pmd.model.config.thirdpartycplocation.file;

import java.net.URL;
import java.util.Objects;

import software.xdev.pmd.model.config.thirdpartycplocation.ThirdPartyCPLocation;
import software.xdev.pmd.model.config.thirdpartycplocation.ThirdPartyCPLocationType;


public abstract class FileThirdPartyCPLocation extends ThirdPartyCPLocation
{
	private final String location; // Used for storage
	private final String absolutePath; // Used for UI (file-picker)
	
	protected FileThirdPartyCPLocation(
		final ThirdPartyCPLocationType type,
		final String id,
		final URL url,
		final String location,
		final String absolutePath)
	{
		super(type, id, url);
		this.location = Objects.requireNonNull(location);
		this.absolutePath = Objects.requireNonNull(absolutePath);
	}
	
	public String location()
	{
		return this.location;
	}
	
	public String absolutePath()
	{
		return this.absolutePath;
	}
	
	@Override
	public String displayLocation()
	{
		return this.location();
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
		final FileThirdPartyCPLocation that = (FileThirdPartyCPLocation)o;
		return Objects.equals(this.location, that.location);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), this.location);
	}
}
