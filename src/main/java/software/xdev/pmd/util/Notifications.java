package software.xdev.pmd.util;

import static com.intellij.notification.NotificationType.ERROR;
import static com.intellij.notification.NotificationType.INFORMATION;
import static com.intellij.notification.NotificationType.WARNING;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.openapi.project.Project;


public final class Notifications
{
	private Notifications()
	{
	}
	
	public static void showInfo(
		final Project project,
		final String infoText,
		final NotificationAction action)
	{
		balloonGroup()
			.createNotification("", infoText, INFORMATION)
			.addAction(action)
			.notify(project);
	}
	
	public static void showWarning(
		final Project project,
		final String warningText)
	{
		balloonGroup()
			.createNotification("", warningText, WARNING)
			.notify(project);
	}
	
	public static void showError(
		final Project project,
		final String errorText)
	{
		balloonGroup()
			.createNotification("", errorText, ERROR)
			.notify(project);
	}
	
	public static void showException(
		final Project project,
		final Throwable t)
	{
		logOnlyGroup()
			.createNotification(t.toString(), ExceptionUtils.getStackTrace(t), ERROR)
			.notify(project);
	}
	
	public static NotificationGroup balloonGroup()
	{
		return NotificationGroupManager.getInstance()
			.getNotificationGroup("PMDBalloonGroup");
	}
	
	public static NotificationGroup startupHintsGroup()
	{
		return NotificationGroupManager.getInstance()
			.getNotificationGroup("PMDStartupHintsGroup");
	}
	
	public static NotificationGroup logOnlyGroup()
	{
		return NotificationGroupManager.getInstance()
			.getNotificationGroup("PMDLogOnlyGroup");
	}
}
