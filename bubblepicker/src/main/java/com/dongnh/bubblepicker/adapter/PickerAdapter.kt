package com.dongnh.bubblepicker.adapter

import com.dongnh.bubblepicker.model.BubblePickerItem

/**
 * Created by irinagalata on 5/22/17.
 */
interface BubblePickerAdapter {

    val totalCount: Int

    fun getItem(position: Int): BubblePickerItem

}

interface TagPickerAdapter {

        val totalCount: Int

        fun getItem(position: Int): BubblePickerItem
}