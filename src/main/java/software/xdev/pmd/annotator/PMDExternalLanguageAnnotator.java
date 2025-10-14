package software.xdev.pmd.annotator;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.codeInsight.daemon.impl.actions.SuppressFix;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.markdown.utils.doc.DocMarkdownToHtmlConverter;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DefaultProjectFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import net.sourceforge.pmd.lang.rule.Rule;
import net.sourceforge.pmd.reporting.RuleViolation;
import software.xdev.pmd.analysis.PMDAnalysisResult;
import software.xdev.pmd.analysis.PMDAnalyzer;
import software.xdev.pmd.config.ConfigurationLocationSource;
import software.xdev.pmd.util.Notifications;


public class PMDExternalLanguageAnnotator
	extends ExternalAnnotator<PMDExternalLanguageAnnotator.FileInfo, PMDExternalLanguageAnnotator.PMDAnnotations>
{
	private static final Logger LOG = Logger.getInstance(PMDExternalLanguageAnnotator.class);
	
	@Nullable
	@Override
	public FileInfo collectInformation(
		@NotNull final PsiFile psiFile,
		@NotNull final Editor editor,
		final boolean hasErrors)
	{
		return new FileInfo(psiFile, editor.getDocument());
	}
	
	@Nullable
	@Override
	public PMDAnnotations doAnnotate(final FileInfo info)
	{
		final PsiFile file = info.file();
		final Project project = file.getProject();
		try
		{
			return new PMDAnnotations(
				this.analyze(
					file,
					project,
					ProgressManager.getInstance().getProgressIndicator()),
				info.document());
		}
		catch(final Exception ex)
		{
			LOG.error("Failed to annotate", ex);
			Notifications.showException(project, ex);
			return null;
		}
	}
	
	private PMDAnalysisResult analyze(
		final PsiFile file,
		final Project project,
		final ProgressIndicator progress)
	{
		final PMDAnalyzer analyzer = project.getService(PMDAnalyzer.class);
		
		final Module module = ModuleUtilCore.findModuleForFile(file);
		return analyzer.analyze(
			Optional.ofNullable(module),
			Set.of(file),
			project.getService(ConfigurationLocationSource.class).getConfigurationLocations(module),
			progress
		);
	}
	
	@Override
	public void apply(
		@NotNull final PsiFile psiFile,
		final PMDAnnotations annotationResult,
		@NotNull final AnnotationHolder holder)
	{
		if(annotationResult == null)
		{
			return;
		}
		
		final List<RuleViolation> violations = annotationResult.analysisResult().report().getViolations();
		if(violations.isEmpty())
		{
			return;
		}
		
		final InspectionManager inspectionManager = InspectionManager.getInstance(psiFile.getProject());
		
		final Document document = annotationResult.document();
		for(final RuleViolation violation : violations)
		{
			final int startLineOffset = document.getLineStartOffset(violation.getBeginLine() - 1);
			final int endOffset =
				violation.getEndLine() - violation.getBeginLine() > 5 // Only mark first line for long violations
					? document.getLineEndOffset(violation.getBeginLine() - 1)
					: document.getLineStartOffset(violation.getEndLine() - 1) + violation.getEndColumn();
			
			final int startOffset = startLineOffset + violation.getBeginColumn() - 1;
			final PsiElement psiElement = psiFile.findElementAt(startOffset);
			
			try
			{
				final Rule rule = violation.getRule();
				final TextRange range = TextRange.create(startOffset, endOffset);
				
				final String pmdSuffix = "PMD: ";
				
				AnnotationBuilder annotationBuilder = holder.newAnnotation(
						getSeverity(violation),
						pmdSuffix + violation.getDescription())
					.tooltip(
						pmdSuffix + rule.getName()
							+ "<p>"
							+ violation.getDescription()
							+ "</p>"
							+ "<p>"
							+ DocMarkdownToHtmlConverter.convert(
							DefaultProjectFactory.getInstance().getDefaultProject(),
							rule.getDescription())
							+ "</p>")
					.range(range)
					.needsUpdateOnTyping(true);
				
				if(psiElement != null)
				{
					final SuppressFix suppressFix = new SuppressFix("PMD." + rule.getName());
					annotationBuilder = annotationBuilder.newLocalQuickFix(
							suppressFix,
							inspectionManager.createProblemDescriptor(
								psiElement,
								pmdSuffix + rule.getName(),
								suppressFix,
								getProblemHighlightType(violation),
								false))
						.range(range)
						.registerFix();
				}
				
				annotationBuilder
					.withFix(new SupressIntentionAction(violation))
					.create();
			}
			catch(final IllegalArgumentException e)
			{
				// Catching "Invalid range specified" from TextRange.create thrown when file has been updated while
				// analyzing
				LOG.warn("Error while annotating file with PMD warnings", e);
			}
		}
	}
	
	private static HighlightSeverity getSeverity(final RuleViolation violation)
	{
		return switch(violation.getRule().getPriority())
		{
			case HIGH -> HighlightSeverity.ERROR;
			case MEDIUM_HIGH, MEDIUM -> HighlightSeverity.WARNING;
			case MEDIUM_LOW -> HighlightSeverity.WEAK_WARNING;
			case LOW -> HighlightSeverity.INFORMATION;
		};
	}
	
	private static ProblemHighlightType getProblemHighlightType(final RuleViolation violation)
	{
		return switch(violation.getRule().getPriority())
		{
			case HIGH -> ProblemHighlightType.ERROR;
			case MEDIUM_HIGH, MEDIUM -> ProblemHighlightType.WARNING;
			case MEDIUM_LOW -> ProblemHighlightType.WEAK_WARNING;
			case LOW -> ProblemHighlightType.INFORMATION;
		};
	}
	
	public record FileInfo(PsiFile file, Document document)
	{
	}
	
	
	public record PMDAnnotations(PMDAnalysisResult analysisResult, Document document)
	{
	}
}
