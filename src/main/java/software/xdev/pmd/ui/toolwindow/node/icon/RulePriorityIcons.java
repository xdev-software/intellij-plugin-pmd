package software.xdev.pmd.ui.toolwindow.node.icon;

import java.util.EnumMap;
import java.util.Map;

import javax.swing.Icon;

import com.intellij.icons.AllIcons;

import net.sourceforge.pmd.lang.rule.RulePriority;


public final class RulePriorityIcons
{
	private static final Map<RulePriority, Icon> CACHE = createCache();
	
	private static Map<RulePriority, Icon> createCache()
	{
		final EnumMap<RulePriority, Icon> map = new EnumMap<>(RulePriority.class);
		map.put(RulePriority.HIGH, AllIcons.Ide.FatalError);
		map.put(RulePriority.MEDIUM_HIGH, AllIcons.Nodes.ErrorIntroduction);
		map.put(RulePriority.MEDIUM, AllIcons.General.Warning);
		map.put(RulePriority.MEDIUM_LOW, AllIcons.Nodes.WarningIntroduction);
		map.put(RulePriority.LOW, AllIcons.General.Information);
		return map;
	}
	
	public static Icon get(final RulePriority priority)
	{
		return CACHE.getOrDefault(priority, AllIcons.General.ContextHelp);
	}
	
	private RulePriorityIcons()
	{
	}
}
