package software.xdev.pmd.model.config.thirdpartycplocation.file.absolute;

import java.net.URL;

import software.xdev.pmd.model.config.thirdpartycplocation.ThirdPartyCPLocationType;
import software.xdev.pmd.model.config.thirdpartycplocation.file.FileThirdPartyCPLocation;


public class AbsoluteFileThirdPartyCPLocation extends FileThirdPartyCPLocation
{
	protected AbsoluteFileThirdPartyCPLocation(
		final String id,
		final URL url,
		final String location,
		final String absolutePath)
	{
		super(ThirdPartyCPLocationType.ABSOLUTE_FILE, id, url, location, absolutePath);
	}
}
