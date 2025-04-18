/*
 * Copyright (c) 2013 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.android

import android.graphics.Canvas
import android.graphics.Rect
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import com.myopicmobile.textwarrior.common.ColorScheme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

//TODO minimise unnecessary invalidate calls
/**
 * TouchNavigationMethod classes implementing their own carets have to override
 * getCaretBloat() to return the size of the drawing area it needs, in excess of
 * the bounding box of the character the caret is on, and use
 * onTextDrawComplete(Canvas) to draw the caret. Currently, only a fixed size
 * caret is allowed, but scalable carets may be implemented in future.
 */
open class TouchNavigationMethod : SimpleOnGestureListener {
    val _caretBloat: Rect = Rect(0, 0, 0, 0)
    protected var _textField: FreeScrollingTextField? = null
    protected var _isCaretTouched: Boolean = false
    private var _gestureDetector: GestureDetector? = null
    private var lastDist = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var lastSize = 0f
    private var fling = 0
    private var _fastScroll = false

    constructor(textField: FreeScrollingTextField) {
        _textField = textField
        _gestureDetector = GestureDetector(textField.getContext(), this)
        _gestureDetector!!.setIsLongpressEnabled(true)
    }

    @Suppress("unused")
    private constructor()


    open fun getCaretBloat(): Rect {
        return _caretBloat
    }

    override fun onDown(e: MotionEvent): Boolean {
        val x = screenToViewX(e.getX().toInt())
        val y = screenToViewY(e.getY().toInt())
        _isCaretTouched = isNearChar(x, y, _textField!!.caretPosition)
        _fastScroll = x < _textField!!.leftOffset
        if (_textField!!.isFlingScrolling) {
            _textField!!.stopFlingScrolling()
        } else if (_textField!!.isSelectText) {
            if (isNearChar(x, y, _textField!!.selectionStart)) {
                _textField!!.focusSelectionStart()
                _textField!!.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                _isCaretTouched = true
            } else if (isNearChar(x, y, _textField!!.selectionEnd)) {
                _textField!!.focusSelectionEnd()
                _textField!!.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                _isCaretTouched = true
            }
        }

        if (_isCaretTouched) {
            _textField!!.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }

        return true
    }

    override fun onShowPress(e: MotionEvent) {
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        if (_textField!!.isAccessibilityEnabled) {
            _textField!!.showIME(true)
            return true
        }
        val x = screenToViewX(e.getX().toInt())
        val y = screenToViewY(e.getY().toInt())
        val charOffset = _textField!!.coordToCharIndex(x, y)

        if (_textField!!.isSelectText) {
            val strictCharOffset = _textField!!.coordToCharIndexStrict(x, y)
            if (_textField!!.inSelectionRange(strictCharOffset) ||
                isNearChar(x, y, _textField!!.selectionStart) ||
                isNearChar(x, y, _textField!!.selectionEnd)
            ) {
                // do nothing
            } else {
                _textField!!.selectText(false)
                if (strictCharOffset >= 0) {
                    _textField!!.moveCaret(charOffset)
                }
            }
        } else {
            if (charOffset >= 0) {
                _textField!!.moveCaret(charOffset)
            }
        }
        val displayIME = true
        if (displayIME) {
            _textField!!.showIME(true)
        }
        return true
    }

    /**
     * Note that up events from a fling are NOT captured here.
     * Subclasses have to call super.onUp(MotionEvent) in their implementations
     * of onFling().
     *
     *
     * Also, up events from non-primary pointers in a multi-touch situation are
     * not captured here.
     *
     * @param e
     * @return
     */
    open fun onUp(e: MotionEvent?): Boolean {
        _textField!!.stopAutoScrollCaret()
        _isCaretTouched = false
        _fastScroll = false
        lastDist = 0f
        fling = 0
        return true
    }

    override fun onScroll(
        e1: MotionEvent?, e2: MotionEvent, distanceX: Float,
        distanceY: Float
    ): Boolean {
        //onTouchZoon(e2);

        var distanceX = distanceX
        var distanceY = distanceY
        if (_isCaretTouched) {
            dragCaret(e2)
        } else if (e2.getPointerCount() == 1) {
            if (fling == 0) if (abs(distanceX.toDouble()) > abs(distanceY.toDouble())) fling = 1
            else fling = -1
            if (fling == 1) distanceY = 0f
            else if (fling == -1) distanceX = 0f
            if (_fastScroll) distanceY *= (_textField!!.maxScrollY / _textField!!.getHeight()).toFloat()
            scrollView(distanceX, distanceY)

            //_textField.smoothScrollBy((int)distanceX, (int)distanceY);
        }

        //TODO find out if ACTION_UP events are actually passed to onScroll
        if ((e2.getAction() and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            onUp(e2)
        }
        return true
    }

    private fun dragCaret(e: MotionEvent) {
        if (!_textField!!.isSelectText && this.isDragSelect) {
            _textField!!.selectText(true)
        }

        val x = e.getX().toInt() - _textField!!.getPaddingLeft()
        val y = e.getY().toInt() - _textField!!.getPaddingTop()
        var scrolled = false

        // If the edges of the textField content area are touched, scroll in the
        // corresponding direction.
        if (x < SCROLL_EDGE_SLOP) {
            scrolled = _textField!!.autoScrollCaret(FreeScrollingTextField.SCROLL_LEFT)

        } else if (x >= (_textField!!.contentWidth - SCROLL_EDGE_SLOP)) {
            scrolled = _textField!!.autoScrollCaret(FreeScrollingTextField.SCROLL_RIGHT)
        } else if (y < SCROLL_EDGE_SLOP) {
            scrolled = _textField!!.autoScrollCaret(FreeScrollingTextField.SCROLL_UP)
        } else if (y >= (_textField!!.contentHeight - SCROLL_EDGE_SLOP)) {
            scrolled = _textField!!.autoScrollCaret(FreeScrollingTextField.SCROLL_DOWN)
        }

        if (!scrolled) {
            _textField!!.stopAutoScrollCaret()
            val newCaretIndex = _textField!!.coordToCharIndex(
                screenToViewX(e.getX().toInt()),
                screenToViewY(e.getY().toInt())
            )
            if (newCaretIndex >= 0) {
                _textField!!.moveCaret(newCaretIndex)
            }
        }
    }

    private fun scrollView(distanceX: Float, distanceY: Float) {
        var newX = distanceX.toInt() + _textField!!.getScrollX()
        var newY = distanceY.toInt() + _textField!!.getScrollY()

        // If scrollX and scrollY are somehow more than the recommended
        // max scroll values, use them as the new maximum
        // Also take into account the size of the caret,
        // which may extend beyond the text boundaries
        val maxWidth = max(
            _textField!!.maxScrollX.toDouble(),
            _textField!!.getScrollX().toDouble()
        ).toInt()
        if (newX > maxWidth) {
            newX = maxWidth
        } else if (newX < 0) {
            newX = 0
        }

        val maxHeight = max(
            _textField!!.maxScrollY.toDouble(),
            _textField!!.getScrollY().toDouble()
        ).toInt()
        if (newY > maxHeight) {
            newY = maxHeight
        } else if (newY < 0) {
            newY = 0
        }
        //_textField.scrollTo(newX, newY);
        _textField!!.smoothScrollTo(newX, newY)
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        var velocityX = velocityX
        var velocityY = velocityY
        if (!_isCaretTouched) {
            if (fling == 1) velocityY = 0f
            else if (fling == -1) velocityX = 0f

            _textField!!.flingScroll(-velocityX.toInt() * 2, -velocityY.toInt() * 2)
        }
        onUp(e2)
        return true
    }

    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt((x * x + y * y).toDouble()).toFloat()
    }

    private fun onTouchZoom(e: MotionEvent): Boolean {
        if (e.getAction() == MotionEvent.ACTION_MOVE) {
            if (e.getPointerCount() == 2) {
                if (lastDist == 0f) {
                    val x = e.getX(0) - e.getX(1)
                    val y = e.getY(0) - e.getY(1)
                    lastDist = sqrt((x * x + y * y).toDouble()).toFloat()
                    lastX = (e.getX(0) + e.getX(1)) / 2
                    lastY = (e.getY(0) + e.getY(1)) / 2
                    lastSize = _textField!!.textSize
                }

                val dist = spacing(e)
                if (lastDist != 0f) {
                    _textField!!.setTextSize((lastSize * (dist / lastDist)).toInt())
                    //_textField.scrollBy(0,(int)(lastY-lastY*(_textField.getTextSize() / lastSize)));
                }
                //_textField.setTextSize((int)(_textField.getTextSize() * (dist / lastDist)));
                //lastDist = dist;
                return true
            }
        }
        lastDist = 0f
        return false
    }

    /**
     * Subclasses overriding this method have to call the superclass method
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        onTouchZoom(event)
        var handled = _gestureDetector!!.onTouchEvent(event)
        if (!handled
            && (event.getAction() and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP
        ) {
            // propagate up events since GestureDetector does not do so
            handled = onUp(event)
        }
        return handled
    }

    override fun onLongPress(e: MotionEvent) {
        onDoubleTap(e)
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        _isCaretTouched = true
        val x = screenToViewX(e.getX().toInt())
        val y = screenToViewY(e.getY().toInt())
        val charOffset = _textField!!.coordToCharIndex(x, y)

        if (_textField!!.isSelectText && _textField!!.inSelectionRange(charOffset)) {
            val doc = _textField!!.createDocumentProvider()
            val line = doc.findLineNumber(charOffset)
            val start = doc.getLineOffset(line)
            val end = doc.getLineOffset(line + 1) - 1
            _textField!!.setSelectionRange(start, end - start)
        } else {
            if (charOffset >= 0) {
                _textField!!.moveCaret(charOffset)
                val doc = _textField!!.createDocumentProvider()
                var start: Int
                var end: Int
                start = charOffset
                while (start >= 0) {
                    val c = doc.get(start)
                    if (!Character.isJavaIdentifierPart(c)) break
                    start--
                }
                if (start != charOffset) start++
                end = charOffset
                while (end >= 0) {
                    val c = doc.get(end)
                    if (!Character.isJavaIdentifierPart(c)) break
                    end++
                }
                _textField!!.selectText(true)
                _textField!!.setSelectionRange(start, end - start)
            }
        }
        return true
    }

    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return false
    }

    fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return false
    }

    /**
     * Android lifecyle event. See [android.app.Activity.onPause].
     */
    fun onPause() {
        //do nothing
    }

    /**
     * Android lifecyle event. See [android.app.Activity.onResume].
     */
    fun onResume() {
        //do nothing
    }

    /**
     * Called by FreeScrollingTextField when it has finished drawing text.
     * Classes extending TouchNavigationMethod can use this to draw, for
     * example, a custom caret.
     *
     *
     * The canvas includes padding in it.
     *
     * @param canvas
     */
    open fun onTextDrawComplete(canvas: Canvas?) {
        // Do nothing. Basic caret drawing is handled by FreeScrollingTextField.
    }

    open fun onColorSchemeChanged(colorScheme: ColorScheme?) {
        // Do nothing. Derived classes can use this to change their graphic assets accordingly.
    }


    //*********************************************************************
    //**************************** Utilities ******************************
    //*********************************************************************
    fun onChiralityChanged(isRightHanded: Boolean) {
        // Do nothing. Derived classes can use this to change their input
        // handling and graphic assets accordingly.
    }

    protected fun getPointerId(e: MotionEvent): Int {
        return ((e.getAction() and MotionEvent.ACTION_POINTER_ID_MASK)
                shr MotionEvent.ACTION_POINTER_ID_SHIFT)
    }

    /**
     * Converts a x-coordinate from screen coordinates to local coordinates,
     * excluding padding
     */
    protected fun screenToViewX(x: Int): Int {
        return x - _textField!!.getPaddingLeft() + _textField!!.getScrollX()
    }

    /**
     * Converts a y-coordinate from screen coordinates to local coordinates,
     * excluding padding
     */
    protected fun screenToViewY(y: Int): Int {
        return y - _textField!!.getPaddingTop() + _textField!!.getScrollY()
    }

    val isRightHanded: Boolean
        get() = true

    private val isDragSelect: Boolean
        get() = false

    /**
     * Determine if a point(x,y) on screen is near a character of interest,
     * specified by its index charOffset. The radius of proximity is defined
     * by TOUCH_SLOP.
     *
     * @param x          X-coordinate excluding padding
     * @param y          Y-coordinate excluding padding
     * @param charOffset the character of interest
     * @return Whether (x,y) lies close to the character with index charOffset
     */
    fun isNearChar(x: Int, y: Int, charOffset: Int): Boolean {
        val bounds = _textField!!.getBoundingBox(charOffset)

        return (y >= (bounds.top - TOUCH_SLOP) && y < (bounds.bottom + TOUCH_SLOP) && x >= (bounds.left - TOUCH_SLOP) && x < (bounds.right + TOUCH_SLOP)
                )
    }

    companion object {
        open val caretBloat: Rect = Rect(0, 0, 0, 0)
            /**
             * For any printed character, this method returns the amount of space
             * required in excess of the bounding box of the character to draw the
             * caret.
             * Subclasses should override this method if they are drawing their
             * own carets.
             */
            get() = field

        // When the caret is dragged to the edges of the text field, the field will
        // scroll automatically. SCROLL_EDGE_SLOP is the width of these edges in pixels
        // and extends inside the content area, not outside to the padding area
        protected var SCROLL_EDGE_SLOP: Int = 10

        /**
         * The radius, in density-independent pixels, around a point of interest
         * where any touch event within that radius is considered to have touched
         * the point of interest itself
         */
        protected var TOUCH_SLOP: Int = 12
    }
}
