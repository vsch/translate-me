package com.lid.intellij.translateme.actions;

import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;

public class ReverseTranslateAction extends TranslateActionBase {
	static final NotificationGroup NOTIFICATION_GROUP = new NotificationGroup("TranslateMe Alerts", NotificationDisplayType.STICKY_BALLOON, false, null);

	public ReverseTranslateAction() {
		super(true);
	}
}
