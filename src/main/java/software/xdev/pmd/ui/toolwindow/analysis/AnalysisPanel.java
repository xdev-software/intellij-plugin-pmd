package software.xdev.pmd.ui.toolwindow.analysis;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.icons.AllIcons;
import com.intellij.ide.AutoScrollToSourceOptionProvider;
import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.DefaultTreeExpander;
import com.intellij.ide.TreeExpander;
import com.intellij.openapi.Disposable;
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
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.treeStructure.Tree;

import net.sourceforge.pmd.lang.rule.Rule;
import software.xdev.pmd.currentfile.CombinedPMDAnalysisResult;
import software.xdev.pmd.ui.toolwindow.PMDToolWindowFactory;
import software.xdev.pmd.ui.toolwindow.node.BaseNode;
import software.xdev.pmd.ui.toolwindow.node.RootNode;
import software.xdev.pmd.ui.toolwindow.node.has.HasDoNotExpandByDefault;
import software.xdev.pmd.ui.toolwindow.node.has.HasErrorAdapter;
import software.xdev.pmd.ui.toolwindow.node.has.HasRule;
import software.xdev.pmd.ui.toolwindow.node.render.NodeCellRenderer;
import software.xdev.pmd.ui.toolwindow.nodehierarchy.TreeNodeHierarchyBuilderFactory;
import software.xdev.pmd.ui.toolwindow.nodehierarchy.TreeNodeHierarchyFactories;


public abstract class AnalysisPanel extends SimpleToolWindowPanel implements GroupByActionTarget, Disposable
{
	protected final Project project;
	
	protected final DefaultActionGroup toolbarActionGroup;
	
	protected final Tree tree = new AnalysisTree();
	protected final JBScrollPane treeScrollPane = new JBScrollPane(this.tree);
	protected final DefaultTreeModel treeModel = new DefaultTreeModel(new RootNode());
	protected final OnePixelSplitter mainSplit = new OnePixelSplitter(false);
	
	protected final ReentrantLock updateTreeLock = new ReentrantLock();
	protected boolean treeUpdateInProgress;
	
	protected JComponent detailComponent;
	
	protected CombinedPMDAnalysisResult result;
	
	protected PreviousRuleDetailsPanel previousRuleDetailsPanel;
	
	@NotNull
	protected TreeNodeHierarchyBuilderFactory currentHierarchyBuilderFactory;
	
	@SuppressWarnings("checkstyle:MagicNumber")
	protected AnalysisPanel(
		@NotNull final Project project,
		@NotNull final TreeNodeHierarchyBuilderFactory currentHierarchyBuilderFactory)
	{
		super(false);
		
		this.project = project;
		this.currentHierarchyBuilderFactory = currentHierarchyBuilderFactory;
		
		this.toolbarActionGroup = this.createActions();
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
	private JComponent createToolbar()
	{
		final ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(
			PMDToolWindowFactory.TOOL_WINDOW_ID,
			this.toolbarActionGroup,
			false);
		final JComponent toolbarComponent = toolbar.getComponent();
		toolbar.setTargetComponent(this);
		return toolbarComponent;
	}
	
	private DefaultActionGroup createActions()
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
		
		TreeNodeHierarchyFactories.ALL_FACTORIES
			.stream()
			.map(factory -> new GroupByAction(this, factory))
			.forEach(actionGroup::add);
		
		actionGroup.add(new Separator());
		
		final TreeExpander treeExpander = new DefaultTreeExpander(this.tree);
		actionGroup.add(manager.createExpandAllAction(treeExpander, this));
		actionGroup.add(manager.createCollapseAllAction(treeExpander, this));
		
		return actionGroup;
	}
	
	@Override
	public boolean isGroupByActionAvailable()
	{
		return this.result != null && !this.treeUpdateInProgress;
	}
	
	@Override
	@NotNull
	public TreeNodeHierarchyBuilderFactory getCurrentHierarchyBuilderFactory()
	{
		return this.currentHierarchyBuilderFactory;
	}
	
	@Override
	public void setCurrentHierarchyBuilderFactoryAndUpdate(final TreeNodeHierarchyBuilderFactory factory)
	{
		if(this.isGroupByActionAvailable())
		{
			this.currentHierarchyBuilderFactory = factory;
			this.updateTreeInternal();
		}
	}
	
	private void onTreeNodeSelected(final Object node)
	{
		final Optional<DetailComponentInfo> optDetailComponentInfo = this.createDetail(node);
		
		final JComponent newDetailComponent = optDetailComponentInfo
			.map(DetailComponentInfo::component)
			.orElse(null);
		
		if(this.detailComponent != newDetailComponent)
		{
			this.disposeAndRemoveDetailComponent();
			
			optDetailComponentInfo.ifPresent(info -> {
				final Supplier<JBScrollPane> scrollPaneSupplier = info.scrollPaneSupplier();
				if(scrollPaneSupplier == null)
				{
					this.mainSplit.setSecondComponent(info.component());
					return;
				}
				
				final JBScrollPane scrollPane = scrollPaneSupplier.get();
				scrollPane.setViewportView(info.component());
				this.mainSplit.setSecondComponent(scrollPane);
			});
			this.detailComponent = newDetailComponent;
		}
	}
	
	private void disposeAndRemoveDetailComponent()
	{
		if(!(this.detailComponent instanceof RuleDetailPanel))
		{
			this.previousRuleDetailsPanel = null;
		}
		if(this.detailComponent instanceof final Disposable disposable)
		{
			disposable.dispose();
		}
		this.detailComponent = null;
		this.mainSplit.setSecondComponent(null);
	}
	
	private Optional<DetailComponentInfo> createDetail(final Object node)
	{
		if(node instanceof final HasErrorAdapter hasErrorAdapter)
		{
			final String msg = hasErrorAdapter.errorAdapter().allDetails();
			final JBTextArea textArea = new JBTextArea(msg);
			textArea.setEditable(false);
			
			return Optional.of(new DetailComponentInfo(textArea));
		}
		else if(node instanceof final HasRule hasRule)
		{
			final Rule rule = hasRule.getRule();
			
			final RuleDetailPanel ruleDetailPanel;
			if(this.previousRuleDetailsPanel != null && rule.equals(this.previousRuleDetailsPanel.rule()))
			{
				ruleDetailPanel = this.previousRuleDetailsPanel.detailPanel();
			}
			else
			{
				ruleDetailPanel = new RuleDetailPanel(this.project, rule);
				this.previousRuleDetailsPanel = new PreviousRuleDetailsPanel(rule, ruleDetailPanel);
			}
			
			return Optional.of(new DetailComponentInfo(
				ruleDetailPanel,
				() -> new JBScrollPane(
					ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)));
		}
		return Optional.empty();
	}
	
	record DetailComponentInfo(
		@NotNull JComponent component,
		Supplier<JBScrollPane> scrollPaneSupplier
	)
	{
		public DetailComponentInfo(final JComponent component)
		{
			this(component, JBScrollPane::new);
		}
	}
	
	protected void updateTree(final CombinedPMDAnalysisResult result)
	{
		this.result = result;
		this.updateTreeInternal();
	}
	
	protected void updateTreeInternal()
	{
		ApplicationManager.getApplication().executeOnPooledThread(() ->
		{
			this.updateTreeLock.lock();
			this.treeUpdateInProgress = true;
			
			ApplicationManager.getApplication().invokeLater(() ->
				this.mainSplit.setFirstComponent(new JLabel("Building Tree...")));
			try
			{
				RootNode rootNode = null;
				String noAnalysisReason = null;
				if(!this.result.isEmpty())
				{
					rootNode = new RootNode();
					this.currentHierarchyBuilderFactory.createBuilder().apply(this.result)
						.build()
						.forEach(rootNode::add);
					rootNode.executeRecursive(BaseNode::update);
				}
				else if(!this.result.noAnalysisReasons().isEmpty())
				{
					noAnalysisReason = this.result.noAnalysisReasons()
						.iterator()
						.next()
						.getDisplayName();
				}
				
				final RootNode rootNodeFinal = rootNode;
				final String noAnalysisReasonFinal = noAnalysisReason;
				ApplicationManager.getApplication().invokeAndWait(() -> this.updateTreeInUI(
					rootNodeFinal, noAnalysisReasonFinal));
			}
			finally
			{
				this.treeUpdateInProgress = false;
				this.updateTreeLock.unlock();
			}
		});
	}
	
	protected void updateTreeInUI(
		@Nullable final RootNode rootNode,
		@Nullable final String noAnalysisReason)
	{
		this.mainSplit.setSecondComponent(null); // Close Detail panel
		this.mainSplit.setFirstComponent(noAnalysisReason == null
			? this.treeScrollPane
			: this.createNoAnalysisReasonPanel(noAnalysisReason));
		this.treeModel.setRoot(rootNode);
	}
	
	protected JComponent createNoAnalysisReasonPanel(final String noAnalysisReason)
	{
		final JBPanel<?> panel = new JBPanel<>(new VerticalFlowLayout());
		panel.add(new JLabel(noAnalysisReason, AllIcons.General.Information, SwingConstants.LEADING));
		return panel;
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
	
	@Override
	public void dispose()
	{
		this.disposeAndRemoveDetailComponent();
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
	
	
	protected record PreviousRuleDetailsPanel(
		Rule rule,
		RuleDetailPanel detailPanel
	)
	{
	}
}
