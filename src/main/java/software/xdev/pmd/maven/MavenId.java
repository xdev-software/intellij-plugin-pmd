package software.xdev.pmd.maven;

import java.nio.CharBuffer;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;


public record MavenId(
	String groupId,
	String artifactId,
	String version
)
{
	public MavenId
	{
		validateOrThrow(groupId);
		validateOrThrow(artifactId);
		validateOrThrow(version);
	}
	
	// Does a general sanity check according to
	// https://maven.apache.org/guides/mini/guide-naming-conventions.html
	static void validateOrThrow(final String input)
	{
		Objects.requireNonNull(input);
		if(input.isEmpty())
		{
			throw new IllegalArgumentException("input is empty");
		}
		
		if(!CharBuffer.wrap(input).chars()
			.allMatch(c -> c >= 'A' && c <= 'Z'
				|| c >= 'a' && c <= 'z'
				|| Character.isDigit(c)
				|| c == '.'
				|| c == '-'
				|| c == '_'
				|| c == '+'
			))
		{
			throw new IllegalArgumentException("Invalid input: " + input);
		}
	}
	
	@Override
	public @NotNull String toString()
	{
		return this.groupId() + ":" + this.artifactId() + ":" + this.version();
	}
}
