package software.xdev.pmd.ui.toolwindow;

import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.psi.PsiFile;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;

import software.xdev.pmd.currentfile.CombinedPMDAnalysisResult;
import software.xdev.pmd.currentfile.CurrentFileAnalysisListener;
import software.xdev.pmd.currentfile.CurrentFileAnalysisManager;
import software.xdev.pmd.ui.toolwindow.node.BaseNode;
import software.xdev.pmd.ui.toolwindow.node.RootNode;
import software.xdev.pmd.ui.toolwindow.node.render.NodeCellRenderer;


public class CurrentFilePanel extends SimpleToolWindowPanel implements CurrentFileAnalysisListener, Disposable
{
	private final CurrentFileAnalysisManager.ListenerDisposeAction fileAnalysisDisposeAction;
	
	private final Tree tree = new Tree();
	private final DefaultTreeModel treeModel = new DefaultTreeModel(new RootNode());
	
	private final ReentrantLock onChangeLock = new ReentrantLock();
	
	@SuppressWarnings("checkstyle:MagicNumber")
	public CurrentFilePanel(final Project project)
	{
		super(false);
		
		this.tree.setCellRenderer(new NodeCellRenderer());
		this.tree.setModel(this.treeModel);
		this.tree.setRootVisible(false);
		
		final JBScrollPane scrollPane = new JBScrollPane(this.tree);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		
		final OnePixelSplitter mainSplit = new OnePixelSplitter(false); // horizontal
		mainSplit.setFirstComponent(scrollPane);
		mainSplit.setSecondComponent(new JLabel("Hier könnte ihre RuleDescription oder Fehlermeldung stehen!"));
		mainSplit.setProportion(0.7f);
		this.add(mainSplit);
		
		// Register listeners
		final CurrentFileAnalysisManager service = project.getService(CurrentFileAnalysisManager.class);
		this.fileAnalysisDisposeAction = service.registerListener(this);
		
		// Init
		service.explicitlyNotifyListener(this);
	}
	
	@Override
	public void onChange(@Nullable final PsiFile psiFile, final CombinedPMDAnalysisResult result)
	{
		ReadAction.run(() ->
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
					expandTreeNode(
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
	
	private static void expandTreeNode(
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
			expandTreeNode(tree, childNode, path.pathByAddingChild(childNode), level - 1);
		}
	}
	
	@Override
	public void dispose()
	{
		this.fileAnalysisDisposeAction.dispose();
	}
}
