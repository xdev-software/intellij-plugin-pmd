package software.xdev.pmd.model.config.rulesetlocation.file.pmd;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class DefaultRuleSetLoaderCreatorTest
{
	@Test
	void checkReflection()
	{
		Assertions.assertDoesNotThrow(DefaultRuleSetLoaderCreator::initReflection);
		
		Assertions.assertDoesNotThrow(() -> DefaultRuleSetLoaderCreator.createAndLoad(rsl -> rsl
			.loadFromResource("category/java/security.xml")));
	}
}
