package software.xdev.pmd.currentfile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.intellij.psi.PsiFile;

import net.sourceforge.pmd.lang.document.FileId;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;
import software.xdev.pmd.analysis.PMDAnalysisResult;


public record CombinedPMDAnalysisResult(
	List<RuleViolation> violations,
	List<Report.SuppressedViolation> suppressedRuleViolations,
	List<Report.ProcessingError> errors,
	List<Report.ConfigurationError> configErrors,
	Map<FileId, PsiFile> fileIdPsiFiles
)
{
	public static CombinedPMDAnalysisResult combine(final Collection<PMDAnalysisResult> results)
	{
		final List<RuleViolation> violations = new ArrayList<>();
		final List<Report.SuppressedViolation> suppressedRuleViolations = new ArrayList<>();
		final List<Report.ProcessingError> processingErrors = new ArrayList<>();
		final List<Report.ConfigurationError> configErrors = new ArrayList<>();
		final Map<FileId, PsiFile> fileIdPsiFiles = new HashMap<>();
		
		for(final PMDAnalysisResult result : results)
		{
			final Report report = result.report();
			if(report != null)
			{
				violations.addAll(report.getViolations());
				suppressedRuleViolations.addAll(report.getSuppressedViolations());
				processingErrors.addAll(report.getProcessingErrors());
				configErrors.addAll(report.getConfigurationErrors());
			}
			
			fileIdPsiFiles.putAll(result.fileIdPsiFiles());
		}
		
		return new CombinedPMDAnalysisResult(
			Collections.unmodifiableList(violations),
			Collections.unmodifiableList(suppressedRuleViolations),
			Collections.unmodifiableList(processingErrors),
			Collections.unmodifiableList(configErrors),
			Collections.unmodifiableMap(fileIdPsiFiles)
		);
	}
	
	public boolean isEmpty()
	{
		return this.fileIdPsiFiles.isEmpty();
	}
}
