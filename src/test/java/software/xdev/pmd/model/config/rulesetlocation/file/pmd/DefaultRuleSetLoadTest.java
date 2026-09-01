package software.xdev.pmd.model.config.rulesetlocation.file.pmd;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sourceforge.pmd.lang.rule.RuleSetLoader;


class DefaultRuleSetLoadTest
{
	@Test
	void checkReflection()
	{
		Assertions.assertDoesNotThrow(DefaultRuleSetLoad::initReflection);
		
		Assertions.assertDoesNotThrow(() -> DefaultRuleSetLoad.load(
			new RuleSetLoader(),
			rsl -> rsl.loadFromResource("category/java/security.xml")));
	}
}
