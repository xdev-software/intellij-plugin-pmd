package software.xdev.pmd.model.config.rulesetlocation.bundled;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.project.Project;

import net.sourceforge.pmd.lang.rule.RuleSet;
import net.sourceforge.pmd.lang.rule.RuleSetLoader;
import software.xdev.pmd.model.config.rulesetlocation.ConfigurationLocation;
import software.xdev.pmd.model.config.rulesetlocation.ConfigurationType;
import software.xdev.pmd.model.config.rulesetlocation.file.pmd.DefaultRuleSetLoad;


public class BundledConfigurationLocation extends ConfigurationLocation
{
	@NotNull
	private final BundledConfig bundledConfig;
	
	public BundledConfigurationLocation(
		@NotNull final BundledConfig bundledConfig,
		@NotNull final Project project)
	{
		super(bundledConfig.getId(), ConfigurationType.BUNDLED, project);
		
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
		// noop
	}
	
	@Override
	public String getLocation()
	{
		return "(bundled)";
	}
	
	@Override
	public void setDescription(@Nullable final String description)
	{
		// noop
	}
	
	@Override
	public String getDescription()
	{
		return this.bundledConfig.getDescription();
	}
	
	@Override
	public void validate(final ClassLoader classLoader)
	{
		// always valid
	}
	
	@Nullable
	@Override
	protected synchronized RuleSet loadRuleSet(final ClassLoader ignored)
	{
		return DefaultRuleSetLoad.load(
			new RuleSetLoader(),
			rsl -> rsl.loadFromResource(this.getBundledConfig().getLocation()));
	}
	
	@Override
	protected boolean shouldReloadRuleSet(final ClassLoader ignored)
	{
		return false;
	}
	
	@Override
	public boolean isRemovable()
	{
		return false;
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
