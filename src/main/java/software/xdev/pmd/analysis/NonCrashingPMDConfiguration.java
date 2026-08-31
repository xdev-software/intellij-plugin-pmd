package software.xdev.pmd.analysis;

import net.sourceforge.pmd.PMDConfiguration;


/**
 * Works around <a href="https://github.com/pmd/pmd/issues/7013">pmd#7013</a>
 */
public class NonCrashingPMDConfiguration extends PMDConfiguration
{
	@Override
	public String getAuxClasspath()
	{
		return null;
	}
}
