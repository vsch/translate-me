package com.lid.intellij.translateme.actions;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.xml.util.XmlStringUtil;

import java.util.List;

public class TranslateAndReplaceActionBase extends EditorAction {

	protected TranslateAndReplaceActionBase(boolean isReversed) {
		super(new TranslateHandler(new ReplaceActionHandler(isReversed)));
	}

	private static class ReplaceActionHandler implements ActionHandler {
		private final boolean isReversed;

		public ReplaceActionHandler(boolean isReversed) {
			this.isReversed = isReversed;
		}

        @Override
        public boolean isMultiCaret() {
            return true;
        }

        @Override
		public boolean isReversed() {
			return isReversed;
		}

		@Override
		public void handleResult(Editor editor, Caret caret, List<String> originals, List<String> translated) {
			final int selectionStart = caret.getSelectionStart();

			String oldText = caret.getSelectedText() == null ? "" : caret.getSelectedText();
			final String newText = translated.isEmpty() ? oldText : translated.get(0);

			EditorModificationUtil.deleteSelectedText(editor);
			EditorModificationUtil.insertStringAtCaret(editor, newText);
			caret.setSelection(selectionStart, selectionStart + newText.length());
		}

        @Override
        public void handleError(Editor editor) {
            Application app = ApplicationManager.getApplication();
            app.invokeLater(() -> {
                showErrorBalloon(editor, "Failed to translate");
            });
        }

        private void showErrorBalloon(Editor editor, String message) {
            NOTIFICATION_GROUP.createNotification("TranslateMe Error", XmlStringUtil.wrapInHtml(message), NotificationType.ERROR, (notification, event) -> {
            }).notify(null);
        }
    }
}
