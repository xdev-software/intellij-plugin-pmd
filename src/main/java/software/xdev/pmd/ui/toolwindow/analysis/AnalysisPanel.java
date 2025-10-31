package software.xdev.pmd.ui.toolwindow.analysis;

import java.awt.Component;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.jetbrains.annotations.NotNull;

import com.intellij.ide.AutoScrollToSourceOptionProvider;
import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.DefaultTreeExpander;
import com.intellij.ide.TreeExpander;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.actionSystem.UiDataProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.treeStructure.Tree;

import software.xdev.pmd.currentfile.CombinedPMDAnalysisResult;
import software.xdev.pmd.ui.toolwindow.PMDToolWindowFactory;
import software.xdev.pmd.ui.toolwindow.RuleDetailPanel;
import software.xdev.pmd.ui.toolwindow.TreeNodeHierarchyBuilder;
import software.xdev.pmd.ui.toolwindow.node.BaseNode;
import software.xdev.pmd.ui.toolwindow.node.RootNode;
import software.xdev.pmd.ui.toolwindow.node.has.HasDoNotExpandByDefault;
import software.xdev.pmd.ui.toolwindow.node.has.HasErrorAdapter;
import software.xdev.pmd.ui.toolwindow.node.has.HasRule;
import software.xdev.pmd.ui.toolwindow.node.render.NodeCellRenderer;


public abstract class AnalysisPanel extends SimpleToolWindowPanel
{
	protected final Project project;
	
	protected final Tree tree = new AnalysisTree();
	protected final JBScrollPane treeScrollPane = new JBScrollPane(this.tree);
	protected final DefaultTreeModel treeModel = new DefaultTreeModel(new RootNode());
	protected final OnePixelSplitter mainSplit = new OnePixelSplitter(false);
	
	protected final ReentrantLock onChangeLock = new ReentrantLock();
	
	@SuppressWarnings("checkstyle:MagicNumber")
	protected AnalysisPanel(final Project project)
	{
		super(false);
		
		this.project = project;
		
		this.setToolbar(this.createToolbar());
		
		this.tree.setCellRenderer(new NodeCellRenderer());
		this.tree.setModel(this.treeModel);
		// https://docs.oracle.com/javase/tutorial/uiswing/events/treeselectionlistener.html
		this.tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		this.tree.addTreeSelectionListener(ev ->
			this.onTreeNodeSelected(this.tree.getLastSelectedPathComponent()));
		
		this.mainSplit.setFirstComponent(this.treeScrollPane);
		this.mainSplit.setProportion(0.5f);
		this.add(this.mainSplit);
	}
	
	@SuppressWarnings("checkstyle:MagicNumber")
	protected JComponent createToolbar()
	{
		final ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(
			PMDToolWindowFactory.TOOL_WINDOW_ID,
			this.createActions(),
			false);
		final JComponent toolbarComponent = toolbar.getComponent();
		toolbar.setTargetComponent(this);
		return toolbarComponent;
	}
	
	private ActionGroup createActions()
	{
		final DefaultActionGroup actionGroup = new DefaultActionGroup();
		final CommonActionsManager manager = CommonActionsManager.getInstance();
		
		actionGroup.add(manager.installAutoscrollToSourceHandler(
			this.project,
			this.tree,
			new AutoScrollToSourceOptionProvider()
			{
				private boolean scrolling = true;
				
				@Override
				public boolean isAutoScrollMode()
				{
					return this.scrolling;
				}
				
				@Override
				public void setAutoScrollMode(final boolean state)
				{
					this.scrolling = state;
				}
			}));
		
		actionGroup.add(new Separator());
		
		final TreeExpander treeExpander = new DefaultTreeExpander(this.tree);
		actionGroup.add(manager.createCollapseAllAction(treeExpander, this));
		actionGroup.add(manager.createExpandAllAction(treeExpander, this));
		
		return actionGroup;
	}
	
	private void onTreeNodeSelected(final Object node)
	{
		this.mainSplit.setSecondComponent(null);
		
		final Component detailComponent = this.getDetailComponent(node);
		this.mainSplit.setSecondComponent(detailComponent != null
			? new JBScrollPane(
			detailComponent,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
			: null);
	}
	
	private Component getDetailComponent(final Object node)
	{
		if(node instanceof final HasErrorAdapter hasErrorAdapter)
		{
			final String msg = hasErrorAdapter.errorAdapter().allDetails();
			final JBTextArea textArea = new JBTextArea(msg);
			textArea.setEditable(false);
			
			return textArea;
		}
		else if(node instanceof final HasRule hasRule)
		{
			return new RuleDetailPanel(this.project, hasRule.getRule());
		}
		return null;
	}
	
	protected void updateTree(final CombinedPMDAnalysisResult result)
	{
		ApplicationManager.getApplication().executeOnPooledThread(() ->
		{
			this.onChangeLock.lock();
			ApplicationManager.getApplication().invokeLater(() ->
				this.mainSplit.setFirstComponent(new JLabel("Building Tree...")));
			try
			{
				final RootNode rootNode = new RootNode();
				if(!result.isEmpty())
				{
					new TreeNodeHierarchyBuilder(result)
						.build()
						.forEach(rootNode::add);
				}
				
				rootNode.executeRecursive(BaseNode::update);
				
				ApplicationManager.getApplication().invokeLater(() ->
					this.updateTreeInUI(rootNode));
			}
			finally
			{
				this.onChangeLock.unlock();
			}
		});
	}
	
	protected void updateTreeInUI(final RootNode rootNode)
	{
		this.mainSplit.setFirstComponent(this.treeScrollPane);
		this.mainSplit.setSecondComponent(null); // Close Detail panel
		this.treeModel.setRoot(rootNode);
	}
	
	protected static void defaultExpandTreeNode(
		final JTree tree,
		final TreeNode node,
		final TreePath path,
		final int level)
	{
		if(level <= 0)
		{
			return;
		}
		
		tree.expandPath(path);
		
		for(int i = 0; i < node.getChildCount(); ++i)
		{
			final TreeNode childNode = node.getChildAt(i);
			if(!(childNode instanceof HasDoNotExpandByDefault))
			{
				defaultExpandTreeNode(tree, childNode, path.pathByAddingChild(childNode), level - 1);
			}
		}
	}
	
	static class AnalysisTree extends Tree implements UiDataProvider
	{
		@Override
		public void uiDataSnapshot(@NotNull final DataSink dataSink)
		{
			// Required for auto scrolling
			dataSink.set(
				CommonDataKeys.NAVIGATABLE_ARRAY,
				this.getSelectedNodes(BaseNode.class, null));
		}
	}
}
