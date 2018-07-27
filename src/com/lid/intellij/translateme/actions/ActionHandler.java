package com.lid.intellij.translateme.actions;

import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;

import java.util.List;

public interface ActionHandler {
    NotificationGroup NOTIFICATION_GROUP = new NotificationGroup("TranslateMe Alerts", NotificationDisplayType.STICKY_BALLOON, false, null);

	boolean isReversed();
	boolean isMultiCaret();
	void handleResult(Editor editor, Caret caret, List<String> original, List<String> translated);
	void handleError(Editor editor);
}
