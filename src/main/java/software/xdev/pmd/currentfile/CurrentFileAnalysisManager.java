package software.xdev.pmd.currentfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.jetbrains.annotations.NotNull;

import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import software.xdev.pmd.analysis.PMDAnalysisResult;


public class CurrentFileAnalysisManager implements FileEditorManagerListener, Disposable
{
	private static final Logger LOG = Logger.getInstance(CurrentFileAnalysisManager.class);
	
	private final PsiManager psiManager;
	private final List<CurrentFileAnalysisListener> listeners = Collections.synchronizedList(new ArrayList<>());
	private final FileEditorManager fileEditorManager;
	
	@NotNull
	private final AtomicReference<PsiFile> currentlySelectedFile = new AtomicReference<>();
	private final Map<PsiFile, Map<ExternalAnnotator<?, ?>, PMDAnalysisResult>> fileAnalysisResults =
		Collections.synchronizedMap(new HashMap<>());
	
	public CurrentFileAnalysisManager(@NotNull final Project project)
	{
		this.psiManager = PsiManager.getInstance(project);
		this.fileEditorManager = FileEditorManager.getInstance(project);
		project.getMessageBus()
			.connect(this)
			.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this);
	}
	
	public void reportAnalysisResult(
		@NotNull final PsiFile psiFile,
		@NotNull final ExternalAnnotator<?, ?> annotator,
		@NotNull final PMDAnalysisResult analysisResult)
	{
		// Execute internally async
		ApplicationManager.getApplication().executeOnPooledThread(() ->
			this.reportAnalysisResultInternal(psiFile, annotator, analysisResult));
	}
	
	private void reportAnalysisResultInternal(
		@NotNull final PsiFile psiFile,
		@NotNull final ExternalAnnotator<?, ?> annotator,
		@NotNull final PMDAnalysisResult analysisResult)
	{
		Map<ExternalAnnotator<?, ?>, PMDAnalysisResult> annotatorResults = this.fileAnalysisResults.get(psiFile);
		if(annotatorResults == null) // We didn't observe that the file was opened, or it was already closed
		{
			// Find currently active file
			final Editor editor = this.fileEditorManager.getSelectedTextEditor();
			if(editor == null)
			{
				this.logAnalysisForNotOpenedFile(psiFile);
				return;
			}
			
			final Optional<PsiFile> optPSIFile = ReadAction.compute(() -> Optional.of(editor)
				.map(Editor::getDocument)
				.map(FileDocumentManager.getInstance()::getFile)
				.flatMap(this::findPSIFile));
			
			optPSIFile.ifPresent(this.currentlySelectedFile::set);
			
			// Check if the currently active file matches the reported analysis result
			if(optPSIFile.filter(psiFile::equals).isEmpty())
			{
				this.logAnalysisForNotOpenedFile(psiFile);
				return;
			}
			
			annotatorResults = this.analysisResultsFor(psiFile);
		}
		
		annotatorResults.put(annotator, analysisResult);
		if(psiFile.equals(this.currentlySelectedFile.get()))
		{
			this.notifyListeners();
		}
	}
	
	private void logAnalysisForNotOpenedFile(final PsiFile psiFile)
	{
		LOG.warn("Analysis was reported for a file[" + psiFile.toString() + "] that is currently not open");
	}
	
	// DO NOT USE fileOpened for currentlySelectedFile
	// As it's reporting opened files in incorrect order during startup
	
	@Override
	public void selectionChanged(@NotNull final FileEditorManagerEvent event)
	{
		final VirtualFile newFile = event.getNewFile();
		if(newFile == null)
		{
			this.currentlySelectedFile.set(null);
			return;
		}
		
		this.findPSIFile(newFile)
			.ifPresentOrElse(
				psiFile -> {
					this.analysisResultsFor(psiFile);
					this.currentlySelectedFile.set(psiFile);
					this.notifyListeners();
				},
				() -> this.currentlySelectedFile.set(null));
	}
	
	@Override
	public void fileClosed(@NotNull final FileEditorManager source, @NotNull final VirtualFile file)
	{
		this.findPSIFile(file)
			.ifPresent(psiFile -> {
				this.fileAnalysisResults.remove(psiFile);
				if(psiFile.equals(this.currentlySelectedFile.get()))
				{
					this.currentlySelectedFile.set(null);
				}
			});
	}
	
	private Optional<PsiFile> findPSIFile(@NotNull final VirtualFile virtualFile)
	{
		return Optional.ofNullable(this.psiManager.findFile(virtualFile));
	}
	
	@NotNull
	private Map<ExternalAnnotator<?, ?>, PMDAnalysisResult> analysisResultsFor(final PsiFile psiFile)
	{
		return this.fileAnalysisResults.computeIfAbsent(
			psiFile,
			ignored -> Collections.synchronizedMap(new LinkedHashMap<>()));
	}
	
	private void notifyListeners()
	{
		final ListenerPayload payload = this.getListenerPayload();
		
		this.listeners.forEach(l -> l.onChange(payload.file(), payload.analysisResults()));
	}
	
	public void explicitlyNotifyListener(@NotNull final CurrentFileAnalysisListener listener)
	{
		final ListenerPayload payload = this.getListenerPayload();
		
		listener.onChange(payload.file(), payload.analysisResults());
	}
	
	@NotNull
	private ListenerPayload getListenerPayload()
	{
		final PsiFile file = this.currentlySelectedFile.get();
		
		final List<PMDAnalysisResult> analysisResults = Optional.ofNullable(file)
			.map(this.fileAnalysisResults::get)
			// Create immutable list
			.map(results -> results.values().stream().toList())
			.orElseGet(List::of);
		return new ListenerPayload(file, CombinedPMDAnalysisResult.combine(analysisResults));
	}
	
	private record ListenerPayload(PsiFile file, CombinedPMDAnalysisResult analysisResults)
	{
	}
	
	public ListenerDisposeAction registerListener(@NotNull final CurrentFileAnalysisListener listener)
	{
		this.listeners.add(listener);
		return () -> this.listeners.remove(listener);
	}
	
	public interface ListenerDisposeAction extends Runnable
	{
		default void dispose()
		{
			this.run();
		}
	}
	
	@Override
	public void dispose()
	{
	}
}
