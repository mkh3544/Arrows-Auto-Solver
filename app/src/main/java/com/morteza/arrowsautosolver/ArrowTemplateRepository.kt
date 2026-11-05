package com.morteza.arrowsautosolver

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

object ArrowTemplateRepository {
    private val names = mapOf(
        ArrowDirection.UP to "arrow_up.png.b64",
        ArrowDirection.DOWN to "arrow_down.png.b64",
        ArrowDirection.LEFT to "arrow_left.png.b64",
        ArrowDirection.RIGHT to "arrow_right.png.b64"
    )

    fun loadAll(context: Context): Map<ArrowDirection, Bitmap> {
        val result = mutableMapOf<ArrowDirection, Bitmap>()
        for ((direction, name) in names) {
            try {
                val encoded = context.assets.open(name).bufferedReader().use { it.readText() }
                val bytes = Base64.decode(encoded.trim(), Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) result[direction] = bitmap
            } catch (_: Exception) {
            }
        }
        return result
    }
}
