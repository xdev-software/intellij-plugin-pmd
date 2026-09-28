package software.xdev.pmd.startup;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;

import kotlin.Unit;
import kotlin.coroutines.Continuation;


public class PMDStartupConfiguration implements ProjectActivity
{
	@SuppressWarnings("PMD.AvoidSystemSetterCall")
	@Nullable
	@Override
	public Object execute(@NotNull final Project project, @NotNull final Continuation<? super Unit> continuation)
	{
		System.setProperty("pmd.error_recovery", "true");
		return null;
	}
}
