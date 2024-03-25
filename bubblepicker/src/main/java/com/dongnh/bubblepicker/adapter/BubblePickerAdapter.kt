package com.dongnh.bubblepicker.adapter

import com.dongnh.bubblepicker.model.PickerItem

/**
 * Created by irinagalata on 5/22/17.
 */
interface BubblePickerAdapter {

    val totalCount: Int
    val mainCount: Int
    val secondaryCount: Int?
        get() = null

    fun getMainItem(position: Int): PickerItem
    fun getSecondaryItem(position: Int): PickerItem { return PickerItem() }
}