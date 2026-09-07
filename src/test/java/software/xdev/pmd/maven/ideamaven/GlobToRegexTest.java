package software.xdev.pmd.maven.ideamaven;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


class GlobToRegexTest
{
	@ParameterizedTest
	@MethodSource
	void checkConversion(final String glob, final String expectedRegex)
	{
		assertEquals(expectedRegex, GlobToRegex.toRegex(glob));
	}
	
	static Stream<Arguments> checkConversion()
	{
		return Map.ofEntries(
				Map.entry("**", ".*"),
				Map.entry("abc", "abc"),
				Map.entry("*/abc", "[^/]*/abc"),
				Map.entry("**/abc", "(?:.*/)?abc"),
				Map.entry("abc/**", "abc/.*"),
				Map.entry("abc/*", "abc/[^/]*"),
				Map.entry("abc/**/*_.java", "abc/(?:.*/)?[^/]*_\\.java")
			)
			.entrySet()
			.stream()
			.map(e -> Arguments.of(e.getKey(), e.getValue()));
	}
}
