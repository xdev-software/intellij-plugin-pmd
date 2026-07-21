package software.xdev.pmd.ui.toolwindow.analysis;

import java.util.Optional;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
		final TreePath startingPath = this.tree.getSelectionPath();
		if(startingPath == null)
		{
			return Optional.empty();
		}
		
		// Forward could have been called on a component that is not a leaf -> select the first leaf
		return Optional.ofNullable(forward && !this.treeModel.isLeaf(startingPath.getLastPathComponent())
			? this.findFirstLeaf(startingPath)
			: this.findSubsequentLeaf(startingPath, forward));
	}
	
	protected TreePath findFirstLeaf(final TreePath startingPath)
	{
		TreePath next = startingPath.pathByAddingChild(
			this.treeModel.getChild(startingPath.getLastPathComponent(), 0));
		while(!this.treeModel.isLeaf(next.getLastPathComponent()))
		{
			next = next.pathByAddingChild(this.treeModel.getChild(next.getLastPathComponent(), 0));
		}
		return next;
	}
	
	@Nullable
	protected TreePath findSubsequentLeaf(
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
				return nextSiblingPath;
			}
			currentPath = parentPath;
			parentPath = currentPath.getParentPath();
		}
		return null;
	}
}
