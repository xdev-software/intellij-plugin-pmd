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
	
	// NOTE: This is the unresolved location that is persisted!
	public abstract void setLocation(final String location);
	
	public abstract String getLocation();
	
	public abstract void setDescription(@Nullable final String description);
	
	public abstract String getDescription();
	
	public boolean isRemovable()
	{
		return true;
	}
	
	public final boolean hasChangedFrom(final ConfigurationLocation configurationLocation)
	{
		return !this.equals(configurationLocation);
	}
	
	@SuppressWarnings("checkstyle:IllegalIdentifierName")
	public void validate(final ClassLoader classLoader) throws Exception
	{
		final RuleSet ruleSet = this.loadRuleSet(classLoader);
		if(ruleSet.getRules().isEmpty())
		{
			throw new IllegalStateException("No rules detected");
		}
		this.cachedRuleSet = ruleSet;
	}
	
	protected abstract RuleSet loadRuleSet(final ClassLoader classLoader) throws Exception;
	
	protected abstract boolean shouldReloadRuleSet(final ClassLoader classLoader);
	
	@Nullable
	public RuleSet getOrRefreshCachedRuleSet(final ClassLoader classLoader)
	{
		if(this.cachedRuleSet == null || this.shouldReloadRuleSet(classLoader))
		{
			this.loadRuleSetSyncIfStillRequired(this.cachedRuleSet, classLoader);
		}
		return this.cachedRuleSet;
	}
	
	protected synchronized void loadRuleSetSyncIfStillRequired(
		final RuleSet expectedRuleSetWhenLoadingStarts,
		final ClassLoader classLoader)
	{
		if(this.cachedRuleSet == null || this.cachedRuleSet == expectedRuleSetWhenLoadingStarts)
		{
			try
			{
				this.cachedRuleSet = this.loadRuleSet(classLoader);
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
		return this.getType() == that.getType()
			&& Objects.equals(this.getLocation(), that.getLocation())
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
		return this.getDescription();
	}
	
	@Override
	public int compareTo(@NotNull final ConfigurationLocation other)
	{
		// bundled configs go last, ordered by their position in the BundledConfig enum
		if(other instanceof final BundledConfigurationLocation otherBundledConfigurationLocation)
		{
			if(this instanceof final BundledConfigurationLocation thisBundledConfigurationLocation)
			{
				return Integer.compare(
					thisBundledConfigurationLocation.getBundledConfig().getSortOrder(),
					otherBundledConfigurationLocation.getBundledConfig().getSortOrder());
			}
			return -1;
		}
		
		if(this instanceof BundledConfigurationLocation)
		{
			return 1;
		}
		
		int result = this.compareStrings(this.getDescription(), other.getDescription());
		if(result == 0)
		{
			result = this.compareStrings(this.getLocation(), other.getLocation());
			if(result == 0)
			{
				return Comparator.nullsFirst(ConfigurationType::compareTo)
					.compare(this.getType(), other.getType());
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
