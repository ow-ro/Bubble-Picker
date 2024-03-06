package com.dongnh.bubblepicker

import com.dongnh.bubblepicker.model.BubblePickerItem

/**
 * Created by irinagalata on 3/6/17.
 */
interface BubblePickerListener {

    fun onBubbleSelected(item: BubblePickerItem)

    fun onBubbleDeselected(item: BubblePickerItem)

}