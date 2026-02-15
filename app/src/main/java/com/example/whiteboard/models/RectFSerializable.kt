package com.example.whiteboard.models

import android.graphics.RectF
import kotlinx.serialization.Serializable

@Serializable
data class RectFSerializable(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f
) {
    fun toRectF() = RectF(left, top, right, bottom)
    companion object {
        fun from(rect: RectF) = RectFSerializable(rect.left, rect.top, rect.right, rect.bottom)
    }
}
