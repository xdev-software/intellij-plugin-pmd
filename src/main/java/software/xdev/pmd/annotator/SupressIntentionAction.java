package software.xdev.pmd.annotator;

import org.jetbrains.annotations.NotNull;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PriorityAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

import net.sourceforge.pmd.reporting.RuleViolation;


public class SupressIntentionAction implements IntentionAction, PriorityAction
{
	@SafeFieldForPreview
	private final RuleViolation violation;
	
	public SupressIntentionAction(final RuleViolation violation)
	{
		this.violation = violation;
	}
	
	@Override
	public @IntentionName @NotNull String getText()
	{
		return "Suppress PMD " + this.violation.getRule().getName();
	}
	
	@Override
	public @NotNull @IntentionFamilyName String getFamilyName()
	{
		return "PMD";
	}
	
	@Override
	public boolean isAvailable(@NotNull final Project project, final Editor editor, final PsiFile file)
	{
		return true;
	}
	
	@Override
	public void invoke(@NotNull final Project project, final Editor editor, final PsiFile file)
	{
		final int offset = editor.getDocument().getLineEndOffset(this.violation.getBeginLine() - 1);
		// Append PMD special comment to end of line.
		editor.getDocument()
			.insertString(
				offset,
				" //NOPMD - suppressed " + this.violation.getRule().getName()
					+ " - TODO explain reason for suppression");
	}
	
	@Override
	public boolean startInWriteAction()
	{
		return true;
	}
	
	@NotNull
	@Override
	public Priority getPriority()
	{
		// Slightly lower priority so that the other "Suppress for member" or similar is prioritized and not this one
		// See DefaultIntentionsOrderProvider for more details
		return Priority.LOW;
	}
}
