package software.xdev.pmd.model.config.file.pmd;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class LoadFromStringRuleSetLoaderWorkaroundTest
{
	@Test
	void checkReflection()
	{
		Assertions.assertDoesNotThrow(LoadFromStringRuleSetLoaderWorkaround::initReflection);
		Assertions.assertTrue(LoadFromStringRuleSetLoaderWorkaround.reflectionUsable);
	}
}
