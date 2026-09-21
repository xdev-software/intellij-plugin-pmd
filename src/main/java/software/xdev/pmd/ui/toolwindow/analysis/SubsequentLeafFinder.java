package software.xdev.pmd.ui.toolwindow.analysis;

import java.util.Optional;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.jetbrains.annotations.NotNull;

import com.intellij.ui.treeStructure.Tree;


public class SubsequentLeafFinder
{
	protected final Tree tree;
	protected final DefaultTreeModel treeModel;
	
	public SubsequentLeafFinder(final Tree tree, final DefaultTreeModel treeModel)
	{
		this.tree = tree;
		this.treeModel = treeModel;
	}
	
	public Optional<TreePath> find(final boolean forward)
	{
		TreePath startingPath = this.tree.getSelectionPath();
		if(startingPath == null)
		{
			// Assume first/last if nothing was selected
			startingPath = this.tree.getPathForRow(forward ? 0 : this.tree.getRowCount() - 1);
			if(this.treeModel.isLeaf(startingPath.getLastPathComponent()))
			{
				return Optional.of(startingPath);
			}
			else if(!forward) // Not a leaf and we are moving backwards -> Select first leaf of this component
			{
				return this.findNestedLeaf(startingPath, false);
			}
		}
		
		// Forward could have been called on a component that is not a leaf -> select the first leaf
		return forward && !this.treeModel.isLeaf(startingPath.getLastPathComponent())
			? this.findNestedLeaf(startingPath, true)
			: this.findSubsequentLeaf(startingPath, forward);
	}
	
	protected Optional<TreePath> findNestedLeaf(final TreePath startingPath, final boolean first)
	{
		TreePath next = startingPath;
		do
		{
			next = next.pathByAddingChild(this.treeModel.getChild(
				next.getLastPathComponent(),
				first
					? 0
					: this.treeModel.getChildCount(next.getLastPathComponent()) - 1));
		}
		while(!this.treeModel.isLeaf(next.getLastPathComponent()));
		return Optional.of(next);
	}
	
	protected Optional<TreePath> findSubsequentLeaf(
		@NotNull final TreePath startingPath,
		final boolean forward)
	{
		TreePath currentPath = startingPath;
		TreePath parentPath = currentPath.getParentPath();
		while(parentPath != null)
		{
			final Object parent = parentPath.getLastPathComponent();
			final int childIndex = this.treeModel.getIndexOfChild(parent, currentPath.getLastPathComponent());
			final int adjacentChildIndex = childIndex + (forward ? 1 : -1);
			if(adjacentChildIndex >= 0 && adjacentChildIndex < this.treeModel.getChildCount(parent))
			{
				final Object nextSibling = this.treeModel.getChild(parent, adjacentChildIndex);
				TreePath nextSiblingPath = parentPath.pathByAddingChild(nextSibling);
				while(!this.treeModel.isLeaf(nextSiblingPath.getLastPathComponent()))
				{
					final int startingChildIndex = forward
						? 0
						: this.treeModel.getChildCount(nextSiblingPath.getLastPathComponent()) - 1;
					nextSiblingPath = nextSiblingPath.pathByAddingChild(
						this.treeModel.getChild(nextSiblingPath.getLastPathComponent(), startingChildIndex));
				}
				return Optional.of(nextSiblingPath);
			}
			currentPath = parentPath;
			parentPath = currentPath.getParentPath();
		}
		return Optional.empty();
	}
}
