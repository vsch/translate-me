package com.lid.intellij.translateme.actions;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.project.Project;
import com.lid.intellij.translateme.configuration.ConfigurationState;
import com.lid.intellij.translateme.configuration.PersistingService;
import com.lid.intellij.translateme.translators.YandexTranslator;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class TranslateHandler extends EditorWriteActionHandler {

    private final ActionHandler mHandler;

    public TranslateHandler(ActionHandler handler) {
        this.mHandler = handler;
    }

    @Override
    public boolean runForAllCarets() {
        return this.mHandler.isMultiCaret();
    }

    @Override
    public void executeWriteAction(Editor editor, @Nullable Caret caret, DataContext dataContext) {
        if (editor == null) {
            return;
        }

        Project project = editor.getProject();
        ConfigurationState state = PersistingService.getInstance().getState();

        if (this.mHandler.isMultiCaret()) {
            String selectedText = editor.getSelectionModel().getSelectedText();
            if (selectedText != null && selectedText.length() > 0) {
                String splitText = state.isSplitCamelCase() ? splitCamelCase(selectedText) : selectedText;
                splitText = state.isSplitUnderscores() ? splitUnderscore(splitText) : splitText;

                String[] langPairs = TranslateAction.getLangPair(mHandler.isReversed());
                boolean autoDetect = TranslateAction.isAutoDetect();
                List<String> translated = new YandexTranslator().translate(splitText, langPairs, autoDetect);
                if (translated != null) {
                    StringBuilder sb = new StringBuilder();
                    int iMax = translated.size();
                    String sep = "";
                    for (int i = 0; i < iMax; i++) {
                        sb.append(sep).append(translated.get(i));
                        sep = "\n";
                    }

                    mHandler.handleResult(editor, caret, Collections.singletonList(selectedText), Collections.singletonList(sb.toString()));
                } else {
                    mHandler.handleError(editor);
                }
            }
        } else {
            ArrayList<String> translations = new ArrayList<>();
            ArrayList<String> originals = new ArrayList<>();
            boolean hadError = false;
            for (Caret caret1 : editor.getCaretModel().getAllCarets()) {
                String selectedText = caret1.getSelectedText();
                if (selectedText != null && selectedText.length() > 0) {
                    originals.add(selectedText);
                    String splitText = state.isSplitCamelCase() ? splitCamelCase(selectedText) : selectedText;
                    splitText = state.isSplitUnderscores() ? splitUnderscore(splitText) : splitText;

                    String[] langPairs = TranslateAction.getLangPair(mHandler.isReversed());
                    boolean autoDetect = TranslateAction.isAutoDetect();
                    List<String> translated = new YandexTranslator().translate(splitText, langPairs, autoDetect);
                    if (translated != null) {
                        StringBuilder sb = new StringBuilder();
                        int iMax = translated.size();
                        String sep = "";
                        for (int i = 0; i < iMax; i++) {
                            sb.append(sep).append(translated.get(i));
                            sep = "\n";
                        }

                        translations.add(sb.toString());
                    } else {
                        hadError = true;
                    }
                }
            }

            if (hadError) {
                mHandler.handleError(editor);
            } else {
                mHandler.handleResult(editor, caret, originals, translations);
            }
        }
    }

    private String splitUnderscore(String splittedText) {
        String[] splitted = splittedText.split("_");
        return arrayToString(splitted);
    }

    private String splitCamelCase(String selectedText) {
        String[] splitted = selectedText.split("(?<=[a-z])(?=[A-Z])");
        return arrayToString(splitted);
    }

    private String arrayToString(String[] splitted) {
        if (splitted.length == 1) {
            return splitted[0];
        }
        StringBuilder builder = new StringBuilder();
        for (String word : splitted) {
            builder.append(word).append(" ");
        }
        return builder.toString();
    }
}
