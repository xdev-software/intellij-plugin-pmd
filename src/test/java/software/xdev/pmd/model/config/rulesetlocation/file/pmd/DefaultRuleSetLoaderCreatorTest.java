package software.xdev.pmd.model.config.rulesetlocation.file.pmd;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sourceforge.pmd.lang.rule.RuleSetLoader;


class DefaultRuleSetLoaderCreatorTest
{
	@Test
	void checkReflection()
	{
		Assertions.assertDoesNotThrow(DefaultRuleSetLoaderCreator::initReflection);
		
		Assertions.assertDoesNotThrow(() -> DefaultRuleSetLoaderCreator.load(
			new RuleSetLoader(),
			rsl -> rsl.loadFromResource("category/java/security.xml")));
	}
}
