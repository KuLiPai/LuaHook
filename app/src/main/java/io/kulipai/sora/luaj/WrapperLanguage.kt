package io.kulipai.sora.luaj

import android.os.Bundle
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference

class WrapperLanguage(
    private val textMateLanguage: TextMateLanguage,
    private val luaLanguage: Language = EmptyLanguage()
) : Language by textMateLanguage {

    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {

        //implement in this

        luaLanguage.requireAutoComplete(content, position, publisher, extraArguments)

        //block thread
        textMateLanguage.requireAutoComplete(
            content,
            position,
            publisher,
            extraArguments
        )

        publisher.updateList()

    }


}