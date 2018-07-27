package com.lid.intellij.translateme.actions;

import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;

public class TranslateAction extends TranslateActionBase {
	static final NotificationGroup NOTIFICATION_GROUP = new NotificationGroup("TranslateMe Alerts", NotificationDisplayType.STICKY_BALLOON, false, null);

	public TranslateAction() {
		super(false);
	}
}
