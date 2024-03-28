package com.dongnh.bubblepicker.adapter

import com.dongnh.bubblepicker.model.PickerItem

/**
 * Created by irinagalata on 5/22/17.
 */
interface BubblePickerAdapter {

    val totalItemCount: Int
    val mainItemCount: Int
    val secondaryItemCount: Int?
        get() = null

    fun getMainItem(position: Int): PickerItem
    fun getSecondaryItem(position: Int): PickerItem { return PickerItem() }
}