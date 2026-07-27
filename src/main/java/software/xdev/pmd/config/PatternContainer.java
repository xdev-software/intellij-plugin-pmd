package software.xdev.pmd.config;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import software.xdev.pmd.external.org.springframework.util.ConcurrentReferenceHashMap;


/**
 * Like {@link Pattern} but sort- and comparable
 */
public record PatternContainer(
	Pattern pattern
) implements Comparable<PatternContainer>
{
	private static final Map<String, Pattern> PATTERN_COMPILE_CACHE = new ConcurrentReferenceHashMap<>(
		ConcurrentReferenceHashMap.ReferenceType.WEAK);
	
	public static PatternContainer tryCreate(final String pattern)
	{
		if(pattern == null || pattern.isBlank())
		{
			return null;
		}
		
		try
		{
			return new PatternContainer(PATTERN_COMPILE_CACHE.computeIfAbsent(pattern, Pattern::compile));
		}
		catch(final PatternSyntaxException pse)
		{
			return null;
		}
	}
	
	public String patternString()
	{
		return this.pattern().pattern();
	}
	
	@Override
	public int compareTo(final PatternContainer o)
	{
		return this.pattern().pattern().compareTo(o.pattern().pattern());
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(!(o instanceof final PatternContainer that))
		{
			return false;
		}
		return Objects.equals(this.pattern().pattern(), that.pattern().pattern());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hashCode(this.pattern().pattern());
	}
}
