package software.xdev.pmd.model.config.thirdpartycplocation;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.UUID;

import software.xdev.pmd.config.state.project.thirdpartycp.ThirdPartyCPLocationState;


public abstract class ThirdPartyCPLocationFactory<
	L extends ThirdPartyCPLocation,
	S extends ThirdPartyCPLocationState,
	U>
{
	public abstract L fromPersisted(S state);
	
	public abstract L fromUI(U uiState);
	
	public URL pathToUrl(final Path path)
	{
		try
		{
			return path.toUri().toURL();
		}
		catch(final MalformedURLException e)
		{
			throw new IllegalArgumentException("Path " + path + " can't be converted to url", e);
		}
	}
	
	protected String newRandomId()
	{
		return UUID.randomUUID().toString();
	}
}
