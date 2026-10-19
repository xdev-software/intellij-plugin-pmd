package software.xdev.pmd.model.config.thirdpartycplocation.file.relative;

import java.net.URL;

import software.xdev.pmd.model.config.thirdpartycplocation.ThirdPartyCPLocationType;
import software.xdev.pmd.model.config.thirdpartycplocation.file.FileThirdPartyCPLocation;


public class RelativeFileThirdPartyCPLocation extends FileThirdPartyCPLocation
{
	protected RelativeFileThirdPartyCPLocation(
		final String id,
		final URL url,
		final String location,
		final String absolutePath)
	{
		super(ThirdPartyCPLocationType.RELATIVE_FILE, id, url, location, absolutePath);
	}
}
