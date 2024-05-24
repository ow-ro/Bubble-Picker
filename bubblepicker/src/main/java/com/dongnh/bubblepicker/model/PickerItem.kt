package com.dongnh.bubblepicker.model

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import com.example.libavif.AvifLoader

/**
 * Created by irinagalata on 1/19/17.
 */
data class PickerItem @JvmOverloads constructor(
    var id: Int? = null,
    var title: String? = null,
    var icon: Drawable? = null,
    var iconOnTop: Boolean = true,
    @ColorInt var color: Int? = null,
    var value: Float = 0f,
    var secondaryValue: Float? = null,
    var overlayAlpha: Float = 0f,
    var typeface: Typeface = Typeface.DEFAULT,
    @ColorInt var textColor: Int? = null,
    var textSize: Float = 40f,
    var imgDrawable: Drawable? = null,
    var animatedFrames: List<AvifLoader.FrameInfo>? = null,
    var showImageOnUnSelected: Boolean = false,
    var isSelected: Boolean = false,
    var isViewBorderSelected: Boolean = false,
    @ColorInt var colorBorderSelected: Int? = null,
    var strokeWidthBorder: Float = 12.5f,
    var isSecondary: Boolean = false,
    var customData: Any? = null
)