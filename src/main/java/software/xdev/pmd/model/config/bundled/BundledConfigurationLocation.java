package software.xdev.pmd.model.config.bundled;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.project.Project;

import net.sourceforge.pmd.lang.rule.RuleSet;
import net.sourceforge.pmd.lang.rule.RuleSetLoader;
import software.xdev.pmd.model.config.ConfigurationLocation;
import software.xdev.pmd.model.config.ConfigurationType;


public class BundledConfigurationLocation extends ConfigurationLocation
{
	@NotNull
	private final BundledConfig bundledConfig;
	
	public BundledConfigurationLocation(
		@NotNull final BundledConfig bundledConfig,
		@NotNull final Project project)
	{
		super(bundledConfig.getId(), ConfigurationType.BUNDLED, project);
		super.setLocation(bundledConfig.getLocation());
		super.setDescription(bundledConfig.getDescription());
		
		this.bundledConfig = bundledConfig;
	}
	
	@NotNull
	public BundledConfig getBundledConfig()
	{
		return this.bundledConfig;
	}
	
	@Override
	public void setLocation(final String location)
	{
		// do nothing, we always use the hard-coded location
	}
	
	@Override
	public void setDescription(@Nullable final String description)
	{
		// do nothing, we always use the hard-coded description
	}
	
	@Override
	public void validate()
	{
		// always valid
	}
	
	@Override
	protected synchronized RuleSet loadRuleSet()
	{
		return new RuleSetLoader().loadFromResource(this.getLocation());
	}
	
	@Override
	protected boolean shouldReloadRuleSet()
	{
		return false;
	}
	
	@Override
	public boolean isRemovable()
	{
		return false;
	}
	
	@Override
	@NotNull
	public BundledConfigurationLocation clone()
	{
		return new BundledConfigurationLocation(this.bundledConfig, this.getProject());
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(!(o instanceof final BundledConfigurationLocation that))
		{
			return false;
		}
		if(!super.equals(o))
		{
			return false;
		}
		return Objects.equals(this.getBundledConfig(), that.getBundledConfig());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), this.getBundledConfig());
	}
}
