package software.xdev.pmd.model.config;

import java.util.Comparator;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import net.sourceforge.pmd.lang.rule.RuleSet;
import software.xdev.pmd.model.config.bundled.BundledConfigurationLocation;


/**
 * Bean encapsulating a configuration source.
 * <p>Note on identity: Configuration locations are considered equal if their descriptor matches. The descriptor
 * consists of type, location, and description text. Properties are not considered.</p>
 * <p>Note on order: Configuration locations are ordered by description text, followed by location and type, except
 * that the bundled configurations always go first.</p>
 */
public abstract class ConfigurationLocation implements Comparable<ConfigurationLocation>
{
	protected final Logger logger;
	
	private final String id;
	private final ConfigurationType type;
	private final Project project;
	private String location;
	private String description;
	
	@SuppressWarnings("checkstyle:IllegalIdentifierName")
	protected RuleSet cachedRuleSet;
	
	protected ConfigurationLocation(
		@NotNull final String id,
		@NotNull final ConfigurationType type,
		@NotNull final Project project)
	{
		this.id = id;
		this.type = type;
		this.project = project;
		
		this.logger = Logger.getInstance(this.getClass());
	}
	
	public boolean canBeResolvedInDefaultProject()
	{
		return true;
	}
	
	protected final Project getProject()
	{
		return this.project;
	}
	
	@NotNull
	public String getId()
	{
		return this.id;
	}
	
	public ConfigurationType getType()
	{
		return this.type;
	}
	
	public String getLocation()
	{
		return this.location;
	}
	
	public String getRawLocation()
	{
		return this.location;
	}
	
	public void setLocation(final String location)
	{
		if(location == null || location.isBlank())
		{
			throw new IllegalArgumentException("A non-blank location is required");
		}
		
		this.location = location;
		if(this.description == null)
		{
			this.description = location;
		}
	}
	
	public String getDescription()
	{
		return this.description;
	}
	
	public void setDescription(@Nullable final String description)
	{
		this.description = description == null ? this.location : description;
	}
	
	public boolean isRemovable()
	{
		return true;
	}
	
	public final boolean hasChangedFrom(final ConfigurationLocation configurationLocation)
	{
		return !this.equals(configurationLocation);
	}
	
	@SuppressWarnings("checkstyle:IllegalIdentifierName")
	public void validate() throws Exception
	{
		final RuleSet ruleSet = this.loadRuleSet();
		if(ruleSet.getRules().isEmpty())
		{
			throw new IllegalStateException("No rules detected");
		}
		this.cachedRuleSet = ruleSet;
	}
	
	protected abstract RuleSet loadRuleSet() throws Exception;
	
	protected abstract boolean shouldReloadRuleSet();
	
	@Nullable
	public RuleSet getOrRefreshCachedRuleSet()
	{
		if(this.cachedRuleSet == null || this.shouldReloadRuleSet())
		{
			this.loadRuleSetSyncIfStillRequired(this.cachedRuleSet);
		}
		return this.cachedRuleSet;
	}
	
	protected synchronized void loadRuleSetSyncIfStillRequired(final RuleSet expectedRuleSetWhenLoadingStarts)
	{
		if(this.cachedRuleSet == null || this.cachedRuleSet == expectedRuleSetWhenLoadingStarts)
		{
			try
			{
				this.cachedRuleSet = this.loadRuleSet();
			}
			catch(final Exception ex)
			{
				this.logger.error("Failed to get RuleSet", ex);
				this.cachedRuleSet = null;
			}
		}
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(!(o instanceof final ConfigurationLocation that))
		{
			return false;
		}
		return this.getType() == that.getType() && Objects.equals(this.getLocation(), that.getLocation())
			&& Objects.equals(this.getDescription(), that.getDescription());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(this.getType(), this.getLocation(), this.getDescription());
	}
	
	@Override
	public String toString()
	{
		return this.description;
	}
	
	@SuppressWarnings("PMD.CognitiveComplexity")
	@Override
	public int compareTo(@NotNull final ConfigurationLocation other)
	{
		int result;
		// bundled configs go first, ordered by their position in the BundledConfig enum
		if(other instanceof final BundledConfigurationLocation otherBundledConfigurationLocation)
		{
			if(this instanceof final BundledConfigurationLocation thisBundledConfigurationLocation)
			{
				final int o1 = thisBundledConfigurationLocation.getBundledConfig().getSortOrder();
				final int o2 = otherBundledConfigurationLocation.getBundledConfig().getSortOrder();
				result = Integer.compare(o1, o2);
			}
			else
			{
				result = 1;
			}
		}
		else
		{
			if(this instanceof BundledConfigurationLocation)
			{
				result = -1;
			}
			else
			{
				result = this.compareStrings(this.getDescription(), other.getDescription());
				if(result == 0)
				{
					result = this.compareStrings(this.getLocation(), other.getLocation());
					if(result == 0)
					{
						result = Comparator.nullsFirst(ConfigurationType::compareTo)
							.compare(this.getType(), other.getType());
					}
				}
			}
		}
		return result;
	}
	
	private int compareStrings(@Nullable final String pStr1, @Nullable final String pStr2)
	{
		int result = 0;
		if(pStr1 != null)
		{
			if(pStr2 != null)
			{
				result = pStr1.compareTo(pStr2);
			}
			else
			{
				result = -1;
			}
		}
		else if(pStr2 != null)
		{
			result = 1;
		}
		return result;
	}
}
