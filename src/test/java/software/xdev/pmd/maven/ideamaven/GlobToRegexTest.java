package software.xdev.pmd.maven.ideamaven;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


class GlobToRegexTest
{
	@ParameterizedTest
	@MethodSource
	void checkConversion(final String glob, final Set<String> expectedRegexes)
	{
		assertTrue(expectedRegexes.contains(GlobToRegex.toRegex(glob)));
	}
	
	static Stream<Arguments> checkConversion()
	{
		return Map.ofEntries(
				Map.entry("**", Set.of(".*")),
				Map.entry("abc", Set.of("abc")),
				Map.entry("*/abc", Set.of("[^/]*/abc")),
				Map.entry(
					"**/abc",
					Set.of(
						".*/abc", // 262
						"(?:.*/)?abc" // 263
					)
				),
				Map.entry("abc/**", Set.of("abc/.*")),
				Map.entry("abc/*", Set.of("abc/[^/]*")),
				Map.entry(
					"abc/**/*_.java",
					Set.of(
						"abc(?:/|/.*/)[^/]*_\\.java", // 262
						"abc/(?:.*/)?[^/]*_\\.java" // 263
					)
				)
			)
			.entrySet()
			.stream()
			.map(e -> Arguments.of(e.getKey(), e.getValue()));
	}
}
