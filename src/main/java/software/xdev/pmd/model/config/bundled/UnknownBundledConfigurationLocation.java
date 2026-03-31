package software.xdev.pmd.model.config.bundled;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.project.Project;

import net.sourceforge.pmd.lang.rule.RuleSet;


public class UnknownBundledConfigurationLocation extends BundledConfigurationLocation
{
	public UnknownBundledConfigurationLocation(
		final @NotNull String id,
		final @NotNull Project project)
	{
		super(BundledConfig.createUnknownDummy(id), project);
	}
	
	@Override
	public boolean isRemovable()
	{
		return true;
	}
	
	@Override
	protected synchronized RuleSet loadRuleSet()
	{
		return null;
	}
	
	@Override
	public @Nullable RuleSet getOrRefreshCachedRuleSet()
	{
		return null;
	}
}
