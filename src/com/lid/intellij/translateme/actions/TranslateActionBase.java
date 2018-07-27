package com.lid.intellij.translateme.actions;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.impl.view.FontLayoutService;
import com.intellij.ui.LightweightHint;
import com.intellij.xml.util.XmlStringUtil;
import com.lid.intellij.translateme.ResultDialog;
import com.lid.intellij.translateme.configuration.ConfigurationState;
import com.lid.intellij.translateme.configuration.PersistingService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static javax.swing.event.HyperlinkEvent.EventType.ACTIVATED;

abstract class TranslateActionBase extends EditorAction {
	public TranslateActionBase(boolean isReversed) {
		super(new TranslateHandler(new PopupActionHandler(isReversed)));
	}

	public static boolean isAutoDetect() {
		ConfigurationState state = PersistingService.getInstance().getState();
		return state.isAutoDetect();
	}

	public static boolean isTranslationTooltip() {
		ConfigurationState state = PersistingService.getInstance().getState();
		return state.isTranslationTooltip();
	}

	public static String[] getLangPair(boolean isReversed) {
		ConfigurationState state = PersistingService.getInstance().getState();

		String from = state.getLangFrom();
		String to = state.getLangTo();
		return isReversed ? new String[]{to, from} : new String[]{from, to};
	}

	protected static class PopupActionHandler implements ActionHandler {
		private final boolean isReversed;

		public PopupActionHandler(boolean isReversed) {
			this.isReversed = isReversed;
		}

		@Override
		public boolean isMultiCaret() {
			return false;
		}

		@Override
		public boolean isReversed() {
			return isReversed;
		}

		@Override
		public void handleResult(Editor editor, Caret caret, List<String> originals, List<String> translated) {
			Application app = ApplicationManager.getApplication();
			app.invokeLater(() -> {
				if (isTranslationTooltip()) {
					StringBuilder sb = new StringBuilder();
					int iMax = translated.size();
					String sep = "";
					if (iMax == 1) {
						for (int i = 0; i < iMax; i++) {
							sb.append(sep).append(translated.get(i));
							sep = "\n<hr>";
						}
					} else {
						for (int i = 0; i < iMax; i++) {
							sb.append(sep).append("<b>").append(originals.get(i)).append("</b>: ").append(translated.get(i));
							sep = "\n<hr>";
						}
					}
					sb.append("<hr><p><a href='http://translate.yandex.com/'>Powered by Yandex.Translator</a>");
					showTooltip(editor, sb.toString());
				} else {
					ResultDialog resultDialog = new ResultDialog("Translated", translated);
					resultDialog.setVisible(true);
				}
			});
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

		static int getVisibleAreaWidth(Editor editor) {
			Insets insets = editor.getContentComponent().getInsets();
			int width = Math.max(0, editor.getScrollingModel().getVisibleArea().width - insets.left - insets.right);
			return width;
		}

		private void showTooltip(Editor editor, String message) {
			//String text = "<table><tr><td>&nbsp;</td><td>$message</td><td>&nbsp;</td></tr></table>"
			HintManagerImpl hintManager = (HintManagerImpl) HintManager.getInstance();
			int flags = HintManager.HIDE_BY_CARET_MOVE | HintManager.UPDATE_BY_SCROLLING | HintManager.HIDE_BY_ESCAPE;
			int timeout = 60000; // default 1min?
			String html = HintUtil.prepareHintText(message, HintUtil.getInformationHint());

			JComponent label = HintUtil.createInformationLabel(html, e -> {
				if (e.getEventType() == ACTIVATED) {
					BrowserUtil.browse("http://translate.yandex.com/");
				}
			}, null, null);

			LightweightHint hint = new LightweightHint(label);

			CharSequence chars = editor.getDocument().getCharsSequence();
			int minLine = editor.getDocument().getLineCount();
			int minStart = chars.length();
			int maxEnd = 0;
			int maxColumn = 0;
			int minColumn = chars.length();

			for (Caret caret1 : editor.getCaretModel().getAllCarets()) {
				if (minStart > caret1.getSelectionStart()) {
					minStart = caret1.getSelectionStart();
				}

				if (maxEnd > caret1.getSelectionEnd()) {
					maxEnd = caret1.getSelectionEnd();
				}

				VisualPosition start = editor.offsetToVisualPosition(caret1.getSelectionStart());
				VisualPosition end = editor.offsetToVisualPosition(caret1.getSelectionEnd());

				if (minLine > start.line) {
					minLine = start.line;
				}

				if (maxColumn < start.column) {
					maxColumn = start.column;
				}
				if (minColumn > start.column) {
					minColumn = start.column;
				}

				if (maxColumn < end.column) {
					maxColumn = end.column;
				}
				if (minColumn > end.column) {
					minColumn = end.column;
				}

				int i = caret1.getSelectionStart();
				int iMax = caret1.getSelectionEnd();
				for (; i < iMax; ++i) {
					if (i > 0 && chars.charAt(i) == '\n') {
						VisualPosition vis = editor.offsetToVisualPosition(i - 1);
						if (maxColumn < vis.column) {
							maxColumn = vis.column;
						}
						if (minColumn > vis.column) {
							minColumn = vis.column;
						}
					}
				}
			}

			VisualPosition start = editor.offsetToVisualPosition(minStart);
			VisualPosition end = editor.offsetToVisualPosition(maxEnd);

			FontMetrics fm = editor.getContentComponent().getFontMetrics(editor.getColorsScheme().getFont(EditorFontType.PLAIN));
			int plainSpaceWidth = FontLayoutService.getInstance().charWidth(fm, ' ');
			int width = maxColumn - minColumn;

			if (plainSpaceWidth > 0) {
				width = getVisibleAreaWidth(editor) / plainSpaceWidth;
			}

			VisualPosition pos = new VisualPosition(minLine, (minColumn + Math.min(maxColumn, width)) / 2, false);

			Point p = hintManager.getHintPosition(hint, editor, pos, HintManager.ABOVE);
			hintManager.showEditorHint(hint, editor, p, flags, timeout, false);
		}
	}
}
