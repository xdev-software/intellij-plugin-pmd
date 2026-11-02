package software.xdev.pmd.ui.toolwindow.nodehierarchy;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.intellij.icons.AllIcons;


@SuppressWarnings("java:S2386")
public final class TreeNodeHierarchyFactories
{
	public static final TreeNodeHierarchyBuilderFactory BY_FILE =
		new TreeNodeHierarchyBuilderFactory(
			TreeNodeHierarchyByFileBuilder::new,
			"File",
			AllIcons.Actions.GroupByFile);
	public static final TreeNodeHierarchyBuilderFactory BY_RULE =
		new TreeNodeHierarchyBuilderFactory(
			TreeNodeHierarchyByRuleBuilder::new,
			"Rule",
			AllIcons.Nodes.SortBySeverity);
	
	public static final Set<TreeNodeHierarchyBuilderFactory> ALL_FACTORIES = allFactories();
	
	private TreeNodeHierarchyFactories()
	{
	}
	
	private static Set<TreeNodeHierarchyBuilderFactory> allFactories()
	{
		return new LinkedHashSet<>(List.of(BY_FILE, BY_RULE));
	}
}
