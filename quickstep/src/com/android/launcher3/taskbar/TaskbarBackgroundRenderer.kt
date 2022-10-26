/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.taskbar

import com.android.launcher3.icons.GraphicsUtils.setColorAlphaBound
import com.android.launcher3.Utilities.mapToRange

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.android.launcher3.R
import com.android.launcher3.anim.Interpolators
import com.android.launcher3.util.DisplayController

/**
 * Helps draw the taskbar background, made up of a rectangle plus two inverted rounded corners.
 */
class TaskbarBackgroundRenderer(context: TaskbarActivityContext) {

    val paint: Paint = Paint()
    var backgroundHeight = context.deviceProfile.taskbarSize.toFloat()

    private var maxBackgroundHeight = context.deviceProfile.taskbarSize.toFloat()
    private val transientBackgroundBounds = context.transientTaskbarBounds

    private var shadowBlur = 0f
    private var keyShadowDistance = 0f
    private var bottomMargin = 0

    private val leftCornerRadius = context.leftCornerRadius.toFloat()
    private val rightCornerRadius = context.rightCornerRadius.toFloat()
    private val invertedLeftCornerPath: Path = Path()
    private val invertedRightCornerPath: Path = Path()

    init {
        paint.color = context.getColor(R.color.taskbar_background)
        paint.flags = Paint.ANTI_ALIAS_FLAG
        paint.style = Paint.Style.FILL

        if (DisplayController.isTransientTaskbar(context)) {
            paint.color = context.getColor(R.color.transient_taskbar_background)

            val res = context.resources
            bottomMargin = res.getDimensionPixelSize(R.dimen.transient_taskbar_margin)
            shadowBlur = res.getDimension(R.dimen.transient_taskbar_shadow_blur)
            keyShadowDistance = res.getDimension(R.dimen.transient_taskbar_key_shadow_distance)
        }

        // Create the paths for the inverted rounded corners above the taskbar. Start with a filled
        // square, and then subtract out a circle from the appropriate corner.
        val square = Path()
        square.addRect(0f, 0f, leftCornerRadius, leftCornerRadius, Path.Direction.CW)
        val circle = Path()
        circle.addCircle(leftCornerRadius, 0f, leftCornerRadius, Path.Direction.CW)
        invertedLeftCornerPath.op(square, circle, Path.Op.DIFFERENCE)
        square.reset()
        square.addRect(0f, 0f, rightCornerRadius, rightCornerRadius, Path.Direction.CW)
        circle.reset()
        circle.addCircle(0f, 0f, rightCornerRadius, Path.Direction.CW)
        invertedRightCornerPath.op(square, circle, Path.Op.DIFFERENCE)
    }

    /**
     * Draws the background with the given paint and height, on the provided canvas.
     */
    fun draw(canvas: Canvas) {
        canvas.save()
        canvas.translate(0f, canvas.height - backgroundHeight - bottomMargin)
        if (transientBackgroundBounds.isEmpty) {
            // Draw the background behind taskbar content.
            canvas.drawRect(0f, 0f, canvas.width.toFloat(), backgroundHeight, paint)

            // Draw the inverted rounded corners above the taskbar.
            canvas.translate(0f, -leftCornerRadius)
            canvas.drawPath(invertedLeftCornerPath, paint)
            canvas.translate(0f, leftCornerRadius)
            canvas.translate(canvas.width - rightCornerRadius, -rightCornerRadius)
            canvas.drawPath(invertedRightCornerPath, paint)
        } else {
            val scaleFactor = backgroundHeight / maxBackgroundHeight
            val width = transientBackgroundBounds.width()
            val widthScale = mapToRange(scaleFactor, 0f, 1f, 0.4f, 1f, Interpolators.LINEAR)
            val newWidth = widthScale * width
            val delta = width - newWidth

            // Draw shadow.
            val shadowAlpha = mapToRange(paint.alpha.toFloat(), 0f, 255f, 0f, 25f,
                Interpolators.LINEAR)
            paint.setShadowLayer(shadowBlur, 0f, keyShadowDistance,
                setColorAlphaBound(Color.BLACK, Math.round(shadowAlpha))
            )

            // Draw background.
            val radius = backgroundHeight / 2f;

            canvas.drawRoundRect(
                transientBackgroundBounds.left + (delta / 2f),
                0f,
                transientBackgroundBounds.right - (delta / 2f),
                backgroundHeight,
                radius, radius, paint
            )
        }

        canvas.restore()
    }
}
