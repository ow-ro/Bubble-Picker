package com.dongnh.bubblepicker.model

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt

/**
 * Created by irinagalata on 1/19/17.
 */

interface PickerItem {
    val title: String?
    val color: Int?
    val gradient: BubbleGradient?
    val typeface: Typeface
    val textColor: Int?
    val textSize: Float
    val isSelected: Boolean
    val isViewBorderSelected: Boolean
}

data class BubblePickerItem @JvmOverloads constructor(
    override var title: String? = null,
    var icon: Drawable? = null,
    var iconOnTop: Boolean = true,
    @ColorInt override var color: Int? = null,
    override var gradient: BubbleGradient? = null,
    var radius: Float = 0f,
    var overlayAlpha: Float = 0f,
    override var typeface: Typeface = Typeface.DEFAULT,
    @ColorInt override var textColor: Int? = null,
    override var textSize: Float = 40f,
    var imgDrawable: Drawable? = null,
    var showImageOnUnSelected: Boolean = false,
    override var isSelected: Boolean = false,
    override var isViewBorderSelected: Boolean = false,
    @ColorInt var colorBorderSelected: Int? = null,
    var strokeWidthBorder: Float = 10f,
    var customData: Any? = null
) : PickerItem

data class TagPickerItem @JvmOverloads constructor(
    override var title: String? = null,
    @ColorInt override var color: Int? = null,
    override var gradient: BubbleGradient? = null,
    var size: Float = 0f,
    override var typeface: Typeface = Typeface.DEFAULT,
    @ColorInt override var textColor: Int? = null,
    override var textSize: Float = 40f,
    override var isSelected: Boolean = false,
    override var isViewBorderSelected: Boolean = false,
) : PickerItem
