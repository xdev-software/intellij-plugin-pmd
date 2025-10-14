package software.xdev.pmd.model.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Properties;


public final class PMDBuiltInRulesFinder
{
	private static final String RULESETS_FILENAMES_KEY = "rulesets.filenames";
	private static final String RULE_DELIMITER = ",";
	
	public static List<String> find(final String languageId)
	{
		final Properties props = new Properties();
		final String ruleSetsPropertyFile = "category/" + languageId + "/categories.properties";
		try
		{
			props.load(PMDBuiltInRulesFinder.class.getClassLoader().getResourceAsStream(ruleSetsPropertyFile));
		}
		catch(final IOException e)
		{
			throw new UncheckedIOException("Failed to load rule set property file: " + ruleSetsPropertyFile, e);
		}
		return List.of(props.getProperty(RULESETS_FILENAMES_KEY).split(RULE_DELIMITER));
	}
	
	private PMDBuiltInRulesFinder()
	{
	}
}
