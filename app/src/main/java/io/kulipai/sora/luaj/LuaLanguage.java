package io.kulipai.sora.luaj;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import java.util.List;

import io.github.rosemoe.sora.lang.EmptyLanguage;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lang.QuickQuoteHandler;
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import io.github.rosemoe.sora.lang.completion.CompletionCancelledException;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion;
import io.github.rosemoe.sora.lang.format.Formatter;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.widget.SymbolPairMatch;

public interface LuaLanguage extends Language {
    @NonNull
    default AnalyzeManager getAnalyzeManager() {
        return new EmptyLanguage.EmptyAnalyzeManager();
    }


    default int getInterruptionLevel() {
        return 0;
    }

    @WorkerThread
    default void requireAutoComplete(@NonNull ContentReference var1, @NonNull CharPosition var2, @NonNull CompletionPublisher var3, @NonNull Bundle var4) throws CompletionCancelledException {

    }

    default int getIndentAdvance(@NonNull ContentReference var1, int var2, int var3) {
        return 0;
    }

    @UiThread
    default int getIndentAdvance(@NonNull ContentReference content, int line, int column, int spaceCountOnLine, int tabCountOnLine) {
        return this.getIndentAdvance(content, line, column);
    }

    @UiThread
    default boolean useTab() {
        return false;
    }

    @UiThread
    @NonNull
    default Formatter getFormatter() {
        return new EmptyLanguage.EmptyFormatter();
    }

    @UiThread
    default SymbolPairMatch getSymbolPairs() {
        return EmptyLanguage.EMPTY_SYMBOL_PAIRS;
    }

    @UiThread
    @Nullable
    default NewlineHandler[] getNewlineHandlers() {
        return new NewlineHandler[0];
    }

    @UiThread
    @Nullable
    default QuickQuoteHandler getQuickQuoteHandler() {
        return null;
    }

    @Override
    default void destroy() {

    }

    default void setOnDiagnosticListener(OnDiagnosticListener listener) {

    }

    interface OnDiagnosticListener {
        void onDiagnosticsChanged(List<DiagnosticRegion> regions);
    }
}
