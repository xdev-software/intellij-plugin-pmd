package software.xdev.pmd.ui.toolwindow;

import java.util.List;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.psi.PsiFile;

import software.xdev.pmd.analysis.PMDAnalysisResult;
import software.xdev.pmd.currentfile.CurrentFileAnalysisListener;
import software.xdev.pmd.currentfile.CurrentFileAnalysisManager;


public class CurrentFilePanel extends SimpleToolWindowPanel implements CurrentFileAnalysisListener, Disposable
{
	private final CurrentFileAnalysisManager.ListenerDisposeAction fileAnalysisDisposeAction;
	
	public CurrentFilePanel(final Project project)
	{
		super(false);
		final CurrentFileAnalysisManager service = project.getService(CurrentFileAnalysisManager.class);
		this.fileAnalysisDisposeAction = service.registerListener(this);
		
		// Init
		service.explicitlyNotifyListener(this);
	}
	
	@Override
	public void onAnalyzed(final PsiFile psiFile, final List<PMDAnalysisResult> results)
	{
		System.out.println(psiFile + " " + results);
	}
	
	@Override
	public void dispose()
	{
		this.fileAnalysisDisposeAction.dispose();
	}
}
