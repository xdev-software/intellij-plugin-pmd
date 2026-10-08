package software.xdev.pmd.analysis;

public enum NoAnalysisReason
{
	NO_FILES("No files"),
	NO_CONFIG_LOCATION_OR_EXCLUDED("No configuration location set or excluded"),
	NO_APPLICABLE_FILES("No applicable files");
	
	final String displayText;
	
	NoAnalysisReason(final String displayText)
	{
		this.displayText = displayText;
	}
	
	public String getDisplayName()
	{
		return this.displayText;
	}
}
