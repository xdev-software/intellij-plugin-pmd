package software.xdev.pmd.config.state.project;

import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Text;


public class ConfigurationLocationState
{
	@Attribute
	String id;
	@Attribute
	String type;
	@Attribute
	String scope;
	@Attribute
	String description;
	@Text
	String location;
	
	@SuppressWarnings("unused")
	public ConfigurationLocationState()
	{
		// for serialisation
	}
	
	public ConfigurationLocationState(
		final String id,
		final String type,
		final String location,
		final String description,
		final String scope)
	{
		this.id = id;
		this.type = type;
		this.scope = scope;
		this.description = description;
		this.location = location;
	}
	
	@Override
	public String toString()
	{
		return "ConfigurationLocation{"
			+ "id='" + this.id + '\''
			+ ", type='" + this.type + '\''
			+ ", scope='" + this.scope + '\''
			+ ", description='" + this.description + '\''
			+ ", location='" + this.location + '\''
			+ '}';
	}
}
