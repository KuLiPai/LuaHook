/*
 * Copyright (c) 2013 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.common

import com.myopicmobile.textwarrior.common.Document.TextFieldMetrics
import kotlin.math.min

/**
 * Iterator class to access characters of the underlying text buffer.
 *
 * The usage procedure is as follows:
 * 1. Call seekChar(offset) to mark the position to start iterating
 * 2. Call hasNext() to see if there are any more char
 * 3. Call next() to get the next char
 *
 * If there is more than 1 DocumentProvider pointing to the same Document,
 * changes made by one DocumentProvider will not cause other DocumentProviders
 * to be notified. Implement a publish/subscribe interface if required.
 */
class DocumentProvider : CharSequence {

    override val length: Int
        get() =  _theText.length

    override fun get(charOffset: Int): Char {
        if (_theText.isValid(charOffset)) {
            return _theText.get(charOffset)
        } else {
            return Language.NULL_CHAR
        }
    }

    /** Current position in the text. Range [ 0, _theText.getTextLength() )  */
    private var _currIndex: Int
    private val _theText: Document

    constructor(metrics: TextFieldMetrics) {
        _currIndex = 0
        _theText = Document(metrics)
    }

    constructor(doc: Document) {
        _currIndex = 0
        _theText = doc
    }

    constructor(rhs: DocumentProvider) {
        _currIndex = 0
        _theText = rhs._theText
    }

    /**
     * Get a substring of up to maxChars length, starting from charOffset
     */
    override fun subSequence(charOffset: Int, maxChars: Int): CharSequence {
        return _theText.subSequence(charOffset, maxChars)
    }


    fun getRow(rowNumber: Int): String {
        return _theText.getRow(rowNumber)
    }

    /**
     * Get the row number that charOffset is on
     */
    fun findRowNumber(charOffset: Int): Int {
        return _theText.findRowNumber(charOffset)
    }

    /**
     * Get the line number that charOffset is on. The difference between a line
     * and a row is that a line can be word-wrapped into many rows.
     */
    fun findLineNumber(charOffset: Int): Int {
        return _theText.findLineNumber(charOffset)
    }

    /**
     * Get the offset of the first character on rowNumber
     */
    fun getRowOffset(rowNumber: Int): Int {
        return _theText.getRowOffset(rowNumber)
    }


    /**
     * Get the offset of the first character on lineNumber. The difference
     * between a line and a row is that a line can be word-wrapped into many rows.
     */
    fun getLineOffset(lineNumber: Int): Int {
        return _theText.getLineOffset(lineNumber)
    }

    /**
     * Sets the iterator to point at startingChar.
     *
     * If startingChar is invalid, hasNext() will return false, and _currIndex
     * will be set to -1.
     *
     * @return startingChar, or -1 if startingChar does not exist
     */
    fun seekChar(startingChar: Int): Int {
        if (_theText.isValid(startingChar)) {
            _currIndex = startingChar
        } else {
            _currIndex = -1
        }
        return _currIndex
    }

    fun hasNext(): Boolean {
        return (_currIndex >= 0 &&
                _currIndex < _theText.textLength)
    }

    /**
     * Returns the next character and moves the iterator forward.
     *
     * Does not do bounds-checking. It is the responsibility of the caller
     * to check hasNext() first.
     *
     * @return Next character
     */
    fun next(): Char {
        val nextChar = _theText.get(_currIndex)
        ++_currIndex
        return nextChar
    }

    /**
     * Inserts c into the document, shifting existing characters from
     * insertionPoint (inclusive) to the right
     *
     * If insertionPoint is invalid, nothing happens.
     */
    fun insertBefore(c: Char, insertionPoint: Int, timestamp: Long) {
        if (!_theText.isValid(insertionPoint)) {
            return
        }

        val a = CharArray(1)
        a[0] = c
        _theText.insert(a, insertionPoint, timestamp, true)
    }

    /**
     * Inserts characters of cArray into the document, shifting existing
     * characters from insertionPoint (inclusive) to the right
     *
     * If insertionPoint is invalid, nothing happens.
     */
    fun insertBefore(cArray: CharArray?, insertionPoint: Int, timestamp: Long) {
        if (!_theText.isValid(insertionPoint) || cArray?.size == 0) {
            return
        }

        _theText.insert(cArray!!, insertionPoint, timestamp, true)
    }

    fun insert(i: Int, s: CharSequence) {
        _theText.insert(charArrayOf(s.get(0)), i, System.nanoTime(), true)
    }

    /**
     * Deletes the character at deletionPoint index.
     * If deletionPoint is invalid, nothing happens.
     */
    fun deleteAt(deletionPoint: Int, timestamp: Long) {
        if (!_theText.isValid(deletionPoint)) {
            return
        }
        _theText.delete(deletionPoint, 1, timestamp, true)
    }


    /**
     * Deletes up to maxChars number of characters starting from deletionPoint
     * If deletionPoint is invalid, or maxChars is not positive, nothing happens.
     */
    fun deleteAt(deletionPoint: Int, maxChars: Int, time: Long) {
        if (!_theText.isValid(deletionPoint) || maxChars <= 0) {
            return
        }
        val totalChars =
            min(maxChars.toDouble(), (_theText.textLength - deletionPoint).toDouble()).toInt()
        _theText.delete(deletionPoint, totalChars, time, true)
    }

    val isBatchEdit: Boolean
        /**
         * Returns true if the underlying text buffer is in batch edit mode
         */
        get() = _theText.isBatchEdit

    /**
     * Signals the beginning of a series of insert/delete operations that can be
     * undone/redone as a single unit
     */
    fun beginBatchEdit() {
        _theText.beginBatchEdit()
    }

    /**
     * Signals the end of a series of insert/delete operations that can be
     * undone/redone as a single unit
     */
    fun endBatchEdit() {
        _theText.endBatchEdit()
    }

    val rowCount: Int
        /**
         * Returns the number of rows in the document
         */
        get() = _theText.rowCount

    /**
     * Returns the number of characters in the row specified by rowNumber
     */
    fun getRowSize(rowNumber: Int): Int {
        return _theText.getRowSize(rowNumber)
    }

    /**
     * Returns the number of characters in the document, including the terminal
     * End-Of-File character
     */
    fun docLength(): Int {
        return _theText.textLength
    }

    //TODO make thread-safe
    /**
     * Removes spans from the document.
     * Beware: Not thread-safe! Another thread may be modifying the same spans
     * returned from getSpans()
     */
    fun clearSpans() {
        _theText.clearSpans()
    }

    var spans: MutableList<Pair?>
        /**
         * Beware: Not thread-safe!
         */
        get() = _theText.spans as MutableList<Pair?>
        /**
         * Sets the spans to use in the document.
         * Spans are continuous sequences of characters that have the same format
         * like color, font, etc.
         *
         * @param spans A collection of Pairs, where Pair.first is the start
         * position of the token, and Pair.second is the type of the token.
         */
        set(spans) {
            _theText.spans = spans as MutableList<Pair>
        }

    fun setMetrics(metrics: TextFieldMetrics) {
        _theText.setMetrics(metrics)
    }

    var isWordWrap: Boolean
        get() = _theText.isWordWrap
        /**
         * Enable/disable word wrap for the document. If enabled, the document is
         * immediately analyzed for word wrap breakpoints, which might take an
         * arbitrarily long time.
         */
        set(enable) {
            _theText.isWordWrap = enable
        }

    /**
     * Analyze the document for word wrap break points. Does nothing if word
     * wrap is disabled for the document.
     */
    fun analyzeWordWrap() {
        _theText.analyzeWordWrap()
    }

    fun canUndo(): Boolean {
        return _theText.canUndo()
    }

    fun canRedo(): Boolean {
        return _theText.canRedo()
    }

    fun undo(): Int {
        return _theText.undo()
    }

    fun redo(): Int {
        return _theText.redo()
    }

    override fun toString(): String {
        // TODO: Implement this method
        return _theText.toString()
    }
}
