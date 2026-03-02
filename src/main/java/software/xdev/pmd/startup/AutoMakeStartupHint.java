package software.xdev.pmd.startup;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.compiler.CompilerWorkspaceConfiguration;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.options.advanced.AdvancedSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import software.xdev.pmd.util.Notifications;


public class AutoMakeStartupHint implements ProjectActivity
{
	@Nullable
	@Override
	public Object execute(@NotNull final Project project, @NotNull final Continuation<? super Unit> continuation)
	{
		record Fix(
			boolean required,
			String msg,
			String fixMsg,
			Runnable fixAction
		)
		{
			void executeFix()
			{
				this.fixAction().run();
			}
		}
		
		final CompilerWorkspaceConfiguration compilerWorkspaceConfig =
			CompilerWorkspaceConfiguration.getInstance(project);
		
		final List<Fix> fixes = Stream.of(
				new Fix(
					!compilerWorkspaceConfig.MAKE_PROJECT_ON_SAVE,
					"Auto-build is currently disabled, which means that the compiled classpath "
						+ "will only be updated sporadically",
					"Enable 'Build project automatically'",
					() -> compilerWorkspaceConfig.MAKE_PROJECT_ON_SAVE = true
				),
				new Fix(
					!compilerWorkspaceConfig.allowAutoMakeWhileRunningApplication(),
					"'Allow auto-make to start even if developed application is currently running' is disabled. "
						+ " The classpath will not be updated when the app is running.",
					"Enabled 'Allow auto-make to start even if developed application is currently running'",
					() -> AdvancedSettings.setBoolean("compiler.automake.allow.when.app.running", true)
				))
			.filter(Fix::required)
			.toList();
		
		if(!fixes.isEmpty())
		{
			final Notification notification = Notifications.startupHintsGroup()
				.createNotification(
					"Inaccurate analysis due to auto-build settings",
					"PMD requires an up-to-date classpath for some rules to work properly.<br>"
						+ "The current IDE configuration does not allow for this:<br>"
						+ "<ul>"
						+ fixes.stream()
						.map(fix -> "<li>" + fix.msg() + "</li>")
						.collect(Collectors.joining(""))
						+ "</ul>"
						+ "Some rules (e.g. <code>InvalidLogMessage</code>) might therefore not work properly.",
					NotificationType.WARNING);
			if(fixes.size() > 1)
			{
				notification.addAction(NotificationAction.createExpiring(
					"Fix all",
					(enabledEv, notif) -> fixes.forEach(Fix::executeFix)));
			}
			
			fixes.forEach(fix -> notification.addAction(NotificationAction.createExpiring(
				fix.fixMsg(),
				(enabledEv, notif) -> fix.executeFix()
			)));
			
			notification.notify(project);
		}
		
		return null;
	}
}
