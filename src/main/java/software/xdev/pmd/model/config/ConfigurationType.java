package software.xdev.pmd.model.config;

public enum ConfigurationType
{
	/**
	 * one of the configurations bundled with the Checkstyle tool, chosen from the
	 * {@link software.xdev.pmd.csapi.BundledConfig} enum
	 */
	BUNDLED,
	
	LOCAL_FILE,
	
	/**
	 * Located in a local file where the path is project relative.
	 */
	PROJECT_RELATIVE;
	
	/**
	 * Parse a case-insensitive type string.
	 *
	 * @param typeAsString the type, as a string.
	 * @return the type.
	 */
	public static ConfigurationType parse(final String typeAsString)
	{
		if(typeAsString == null)
		{
			return null;
		}
		
		final String processedType = typeAsString.toUpperCase().replace(' ', '_');
		if("FILE".equals(processedType))
		{
			return LOCAL_FILE;
		}
		
		return valueOf(processedType);
	}
}
