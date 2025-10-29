package software.xdev.pmd.ui.toolwindow.currentfile;

import java.awt.Component;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.treeStructure.Tree;

import software.xdev.pmd.currentfile.CombinedPMDAnalysisResult;
import software.xdev.pmd.currentfile.CurrentFileAnalysisListener;
import software.xdev.pmd.currentfile.CurrentFileAnalysisManager;
import software.xdev.pmd.ui.toolwindow.TreeNodeHierarchyBuilder;
import software.xdev.pmd.ui.toolwindow.node.BaseNode;
import software.xdev.pmd.ui.toolwindow.node.RootNode;
import software.xdev.pmd.ui.toolwindow.node.has.HasDoNotExpandByDefault;
import software.xdev.pmd.ui.toolwindow.node.has.HasErrorAdapter;
import software.xdev.pmd.ui.toolwindow.node.has.HasPositionInFile;
import software.xdev.pmd.ui.toolwindow.node.has.HasRule;
import software.xdev.pmd.ui.toolwindow.node.other.FilePosition;
import software.xdev.pmd.ui.toolwindow.node.render.NodeCellRenderer;


public class CurrentFilePanel extends SimpleToolWindowPanel implements CurrentFileAnalysisListener, Disposable
{
	private final Project project;
	private final FileEditorManager fileEditorManager;
	
	private final CurrentFileAnalysisManager.ListenerDisposeAction fileAnalysisDisposeAction;
	
	private final Tree tree = new Tree();
	private final DefaultTreeModel treeModel = new DefaultTreeModel(new RootNode());
	private final OnePixelSplitter mainSplit = new OnePixelSplitter(false);
	
	private final ReentrantLock onChangeLock = new ReentrantLock();
	
	@SuppressWarnings("checkstyle:MagicNumber")
	public CurrentFilePanel(final Project project)
	{
		super(false);
		
		this.project = project;
		this.fileEditorManager = FileEditorManager.getInstance(project);
		
		this.tree.setCellRenderer(new NodeCellRenderer());
		this.tree.setModel(this.treeModel);
		this.tree.setRootVisible(false);
		// https://docs.oracle.com/javase/tutorial/uiswing/events/treeselectionlistener.html
		this.tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		this.tree.addTreeSelectionListener(ev ->
			this.onTreeNodeSelected(this.tree.getLastSelectedPathComponent()));
		
		final JBScrollPane scrollPane = new JBScrollPane(this.tree);
		
		this.mainSplit.setFirstComponent(scrollPane);
		this.mainSplit.setProportion(0.65f);
		this.add(this.mainSplit);
		
		// Register listeners
		final CurrentFileAnalysisManager service = project.getService(CurrentFileAnalysisManager.class);
		this.fileAnalysisDisposeAction = service.registerListener(this);
		
		// Init
		service.explicitlyNotifyListener(this);
	}
	
	@Override
	public void onChange(@Nullable final PsiFile psiFile, final CombinedPMDAnalysisResult result)
	{
		ApplicationManager.getApplication().executeOnPooledThread(() ->
		{
			this.onChangeLock.lock();
			try
			{
				final RootNode rootNode = new RootNode();
				if(psiFile != null)
				{
					new TreeNodeHierarchyBuilder(result)
						.build()
						.forEach(rootNode::add);
				}
				
				rootNode.executeRecursive(BaseNode::update);
				
				ApplicationManager.getApplication().invokeLater(() ->
				{
					this.treeModel.setRoot(rootNode);
					defaultExpandTreeNode(
						this.tree,
						rootNode,
						new TreePath(this.treeModel.getPathToRoot(rootNode)),
						4);
				});
			}
			finally
			{
				this.onChangeLock.unlock();
			}
		});
	}
	
	private void onTreeNodeSelected(final Object node)
	{
		this.mainSplit.setSecondComponent(null);
		if(node instanceof final HasPositionInFile hasPositionInFile)
		{
			final FilePosition filePosition = hasPositionInFile.filePositionSupplier().get();
			final VirtualFile virtualFile = filePosition.psiFile().getVirtualFile();
			if(virtualFile != null)
			{
				this.fileEditorManager.openTextEditor(
					new OpenFileDescriptor(
						this.project,
						virtualFile,
						filePosition.beginLineIndex(),
						filePosition.beginColumnIndex()
					),
					true);
			}
		}
		
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
	
	private static void defaultExpandTreeNode(
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
		this.fileAnalysisDisposeAction.dispose();
	}
}
