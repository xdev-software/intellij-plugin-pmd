package software.xdev.pmd.model.scope;

import org.jetbrains.annotations.NotNull;


/**
 * Possible values of the 'scope' configuration item.
 */
public enum ScanScope // TODO: Check if this even makes sense
{
	/**
	 * Scan only Java files which reside in source folders (main only)
	 */
	SUPPORTED_ONLY("Only supported sources (but not tests)", true, false),
	
	/**
	 * Scan only Java files which reside in source folders (main <i>and</i> test)
	 */
	SUPPORTED_ONLY_WITH_TESTS("Only supported sources (including tests)", true, true),
	
	/**
	 * Scan all files which reside in source folders (main only)
	 */
	ALL_SOURCES("All sources (but not tests)", false, false),
	
	/**
	 * Scan all files which reside in source folders (main <i>and</i> test)
	 */
	ALL_SOURCES_WITH_TESTS("All sources (including tests)", false, true);
	
	private final String displayName;
	private final boolean supportedSourcesOnly;
	private final boolean withTest;
	
	ScanScope(final String displayName, final boolean supportedSourcesOnly, final boolean withTest)
	{
		this.displayName = displayName;
		this.supportedSourcesOnly = supportedSourcesOnly;
		this.withTest = withTest;
	}
	
	public boolean includeTestClasses()
	{
		return this.withTest;
	}
	
	public boolean includeOnlySupportedSources()
	{
		return this.supportedSourcesOnly;
	}
	
	@NotNull
	public static ScanScope getDefaultValue()
	{
		return SUPPORTED_ONLY_WITH_TESTS;
	}
	
	@Override
	public String toString()
	{
		return this.displayName;
	}
}
