package software.xdev.pmd.ui.toolwindow.analysis.currentfile;

import javax.swing.tree.TreePath;

import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

import software.xdev.pmd.currentfile.CombinedPMDAnalysisResult;
import software.xdev.pmd.currentfile.CurrentFileAnalysisListener;
import software.xdev.pmd.currentfile.CurrentFileAnalysisManager;
import software.xdev.pmd.ui.toolwindow.analysis.AnalysisPanel;
import software.xdev.pmd.ui.toolwindow.node.RootNode;
import software.xdev.pmd.ui.toolwindow.nodehierarchy.TreeNodeHierarchyFactories;


public class CurrentFilePanel extends AnalysisPanel implements CurrentFileAnalysisListener, Disposable
{
	private final CurrentFileAnalysisManager.ListenerDisposeAction fileAnalysisDisposeAction;
	
	public CurrentFilePanel(final Project project)
	{
		super(project, TreeNodeHierarchyFactories.BY_FILE);
		
		this.tree.setRootVisible(false);
		
		// Register listeners
		final CurrentFileAnalysisManager service = project.getService(CurrentFileAnalysisManager.class);
		this.fileAnalysisDisposeAction = service.registerListener(this);
		
		// Init
		service.explicitlyNotifyListener(this);
	}
	
	@Override
	public void onChange(@Nullable final PsiFile psiFile, final CombinedPMDAnalysisResult result)
	{
		this.updateTree(result);
	}
	
	@Override
	protected void updateTreeInUI(final RootNode rootNode)
	{
		super.updateTreeInUI(rootNode);
		defaultExpandTreeNode(
			this.tree,
			rootNode,
			new TreePath(this.treeModel.getPathToRoot(rootNode)),
			4);
	}
	
	@Override
	public void dispose()
	{
		this.fileAnalysisDisposeAction.dispose();
		super.dispose();
	}
}
