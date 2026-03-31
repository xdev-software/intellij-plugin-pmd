package software.xdev.pmd.ui.toolwindow.analysis;

import software.xdev.pmd.ui.toolwindow.nodehierarchy.TreeNodeHierarchyBuilderFactory;


public interface GroupByActionTarget
{
	boolean isGroupByActionAvailable();
	
	TreeNodeHierarchyBuilderFactory getCurrentHierarchyBuilderFactory();
	
	void setCurrentHierarchyBuilderFactoryAndUpdate(TreeNodeHierarchyBuilderFactory factory);
}
