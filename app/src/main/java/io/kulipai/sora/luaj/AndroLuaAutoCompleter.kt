package io.kulipai.sora.luaj

import android.os.Bundle
import io.dingyi222666.sora.lua.tool.LuaLexer
import io.dingyi222666.sora.lua.tool.LuaTokenTypes
import io.dingyi222666.sora.lua.tool.LuaParser
import io.dingyi222666.sora.lua.tool.PackageUtil
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.lang.completion.getCompletionItemComparator
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import org.luaj.LuaError

class AndroLuaAutoCompleter(
    private val language: AndroLuaLanguage
) : AutoCompleter {
    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ): List<DiagnosticRegion> {
        val keyword = computePrefix(content, position)

        val (items, diagnosticRegions) = tokenize(content, publisher)

        val keywords = keyword.split(".","(",")")

        val currentKeyWord = keywords.last()

        val prefixLength = currentKeyWord.length

        filterItems(items, keyword, keywords, currentKeyWord, position)

        val completionList = items.map {
            SimpleCompletionItem(it.name, it.description, prefixLength, it.label.toString())
                .kind(it.type)
        }

        publisher.setComparator(getCompletionItemComparator(content, position, completionList))

        publisher.addItems(completionList)

        publisher.updateList()

        return diagnosticRegions
    }


    private fun filterPackages(
        currentKeyWord: String,
        items: MutableList<CompletionName>,
        keywords: List<String>,
        position: CharPosition
    ) {
        val packageName = keywords.first()

        items.clear()

        if (language.isBasePackage(packageName)) {
            language.getBasePackage(packageName)?.filter {
                it.name.toString().lowercase().startsWith(currentKeyWord)
            }?.let {
                items.addAll(it)
            }
        } else {
            LuaParser.filterJava(packageName, currentKeyWord, position.index)
                .let {
                    items.addAll(it)
                }

            PackageUtil.filterPackage(packageName, currentKeyWord)
                .let {
                    items.addAll(it)
                }
        }
    }

    private fun filterItems(
        items: MutableList<CompletionName>,
        keyword: String,
        keywords: List<String>,
        currentKeyWord: String,
        position: CharPosition
    ) {

        val last = if (keyword.isNotEmpty()) keyword.last() else keyword
        if (keywords.size == 2) {
            filterPackages(currentKeyWord, items, keywords, position)
        } else if (keywords.size == 1) {

            if (last == '.') {
                filterPackages(currentKeyWord, items, keywords, position)
            } else {
                items.addAll(LuaParser.filterLocal(keyword, position.index - 3))

                language.keywords
                    .filter {
                        it.lowercase().indexOf(keyword) == 0
                    }.forEach {
                        items.add(CompletionName(it, CompletionItemKind.Keyword, " :keyword"))
                    }

                language.names
                    .filter {
                        it.lowercase().indexOf(keyword) == 0
                    }.forEach {
                        items.add(CompletionName(it, CompletionItemKind.Function, " :function"))
                    }

                language.packages.keys
                    .filter {
                        it.lowercase().indexOf(keyword) == 0
                    }.forEach {
                        items.add(CompletionName(it, CompletionItemKind.Module, " :package"))
                    }
            }

        }


        PackageUtil.filter(keyword)
            .let {
                items.addAll(it)
            }

    }


    private fun computePrefix(
        text: ContentReference,
        position: CharPosition
    ): String {
        val lineContent = text.getLine(position.line)

        // get the first invisible character
        var firstInvisible = -1

        for (i in position.column - 1 downTo 0) {
            if (isInVisibleChar(lineContent[i])) {
                firstInvisible = i
                break
            }
        }

        return if (firstInvisible == -1) {
            lineContent.take(position.column).lowercase()
        } else {
            lineContent.substring(firstInvisible + 1, position.column).lowercase()
        }
    }


    private fun isInVisibleChar(c: Char): Boolean {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '(' || c == ',' || c == ';'
    }

    private fun tokenize(
        content: ContentReference,
        publisher: CompletionPublisher
    ): Pair<MutableList<CompletionName>, List<DiagnosticRegion>> {
        val items = mutableSetOf<CompletionName>()

        val contentString = content.toString()

//        runCatching {
//            LuaParser.lexer(
//                contentString,
//                publisher
//            )
//        }.onFailure {
//            if (it is LuaError) {
//
//                val line = 1.coerceAtLeast(it.line - 1)
//                val lastLine = 1.coerceAtLeast(it.lastLine - 1)
//                val col = 0.coerceAtLeast(content.getColumnCount(line) - 1)
//                val diagnostics = listOf(
//                    DiagnosticRegion(
//                        content.getCharIndex(lastLine, 0),
//                        content.getCharIndex(line, col),
//                        DiagnosticRegion.SEVERITY_ERROR,
//                        0x00,
//                        DiagnosticDetail("Error", it.message)
//                    )
//                )
//                return Pair(mutableListOf(), diagnostics)
//            }
//
//            return Pair(mutableListOf(), emptyList())
//        }


        val lexer = LuaLexer(contentString)

        // start
        var idx = 0

        var lastType: LuaTokenTypes? = null

        var lastName = ""

        var bul = StringBuilder()
        var isModule = false

        while (true) {
            publisher.checkCancelled();

            val type = lexer.advance() ?: break
            val len = lexer.yylength()
            idx += len

            // check module ?
            if (isModule && lastType == LuaTokenTypes.STRING && type != LuaTokenTypes.STRING) {
                val mod = bul.toString()
                if (bul.length > 2) {
                    val m = mod.substring(1, mod.length - 1)
                    val ms =
                        m.split("[.$]".toRegex()).dropLastWhile { it.isEmpty() }
                            .filter { it != "*" }
                    val na = ms[ms.size - 1]
                    items.add(CompletionName(na, CompletionItemKind.Module, " :import"))
                    LuaParser.addUserWord("$na :import")
                }
                bul = java.lang.StringBuilder()
                isModule = false
            }

            when (type) {
                LuaTokenTypes.STRING, LuaTokenTypes.LONG_STRING -> {
                    //字符串
                    if (lastName == "imports" || lastName == "import") isModule = true

                    if (isModule) bul.append(lexer.yytext())
                }

                LuaTokenTypes.NAME -> {

                    val name = lexer.yytext()
                    if (lastType == LuaTokenTypes.FUNCTION) {
                        //函数名
                        items.add(
                            CompletionName(
                                name,
                                CompletionItemKind.Function,
                                " :function"
                            )
                        )
                    } /*else if (language.isUserWord(name)) {
                        tokens.add(Pair(len, Lexer.LITERAL))
                    } else if (lastType == LuaTokenTypes.GOTO || lastType == LuaTokenTypes.AT) {
                        tokens.add(Pair(len, Lexer.LITERAL))
                    } else if (lastType == LuaTokenTypes.MULT && lastType3 == LuaTokenTypes.LOCAL) {
                        tokens.add(Pair(len, Lexer.OPERATOR))
                    } else if (language.isBasePackage(name)) {
                        tokens.add(Pair(len, Lexer.NAME))
                    } else if (lastType == LuaTokenTypes.DOT && language.isBasePackage(lastName) && language.isBaseWord(
                            lastName,
                            name
                        )
                    ) {
                        //标准库函数
                        tokens.add(Pair(len, Lexer.NAME))
                    } else if (language.isName(name)) {
                        tokens.add(Pair(len, Lexer.NAME))
                    } else {
                        tokens.add(Pair(len, Lexer.NORMAL))
                    }*/

                    /*  if (lastType != LuaTokenTypes.DOT) {
                          val loc = false

                          if (!loc && values.containsKey(name)) {
                              val ls = values[name]
                              for (l in ls!!) {
                                  if (l.first == idx) {
                                      val p: Pair = tokens.get(tokens.size - 1)
                                      val tp = l.second
                                      if (tp == LexState.VVOID) {
                                          if (p.second == Lexer.NORMAL) p.second =
                                              Lexer.GLOBAL
                                          break
                                      } else if (tp == LexState.VUPVAL) {
                                          p.second = Lexer.UPVAL
                                          break
                                      } else if (tp == LexState.VLOCAL) {
                                          p.second = Lexer.LOCAL
                                          break
                                      }
                                  }
                              }
                          }
                      }*/

                    if (lastType == LuaTokenTypes.ASSIGN && name == "imports") {
                        items.add(CompletionName(name, CompletionItemKind.Module, " :imports"))
                        /* if (lastNameIdx >= 0) {
                             val p: Pair = tokens.get(lastNameIdx - 1)
                             p.second = Lexer.LITERAL
                             lastNameIdx = -1
                         }*/
                    }
                    lastName = name
                }

                else -> {}
            }
            if (type != LuaTokenTypes.WHITE_SPACE
            ) {
                lastType = type
            }

        }

        return items.toMutableList() to emptyList()
    }
}