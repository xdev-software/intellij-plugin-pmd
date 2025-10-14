package software.xdev.pmd.model.config;

import net.sourceforge.pmd.lang.rule.RuleSet;
import net.sourceforge.pmd.lang.rule.RuleSetLoader;


public final class PMDRuleSetValidator
{
	public static void validateOrThrow(final String ruleSetContents)
	{
		final RuleSet ruleSet = new RuleSetLoader().loadFromString(
			"tmp-validate-" + System.currentTimeMillis() + ".xml",
			ruleSetContents);
		if(ruleSet.getRules().isEmpty())
		{
			throw new IllegalStateException("No rules detected");
		}
	}
	
	private PMDRuleSetValidator()
	{
	}
}
