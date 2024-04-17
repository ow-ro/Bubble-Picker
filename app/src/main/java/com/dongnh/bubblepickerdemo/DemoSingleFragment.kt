package com.dongnh.bubblepickerdemo

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.dongnh.bubblepicker.BubblePickerListener
import com.dongnh.bubblepicker.adapter.BubblePickerAdapter
import com.dongnh.bubblepicker.model.PickerItem
import com.dongnh.bubblepicker.rendering.BubblePicker
import com.dongnh.bubblepickerdemo.databinding.FragmentSingleDemoBinding
import com.dongnh.bubblepickerdemo.databinding.FragmentSingleDemoBinding.*

class DemoSingleFragment : Fragment() {

    private lateinit var binding: FragmentSingleDemoBinding

    data class Item(
        val title: String,
        val imgResId: Int,
        val value: Float
    )

    lateinit var primaryItems: MutableList<Item>
    lateinit var secondaryItems: MutableList<Item>
    private var firstPicker: BubblePicker? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = inflate(inflater, container, false)
        configView(binding.pickerContainer)
        return binding.root
    }

    @SuppressLint("Recycle")
    private fun configView(container: ViewGroup) {
        val primaryTitles = resources.getStringArray(R.array.countries_primary)
        val primaryImages = resources.obtainTypedArray(R.array.images_primary)
        primaryItems = mutableListOf()
        primaryTitles.forEachIndexed { index, title ->
            primaryItems.add(
                Item(
                    title,
                    primaryImages.getResourceId(index, 0),
                    IntRange(50, 150).random().toFloat()
                )
            )
        }

        val secondaryTitles = resources.getStringArray(R.array.countries_secondary)
        val secondaryImages = resources.obtainTypedArray(R.array.images_secondary)
        secondaryItems = mutableListOf()
        secondaryTitles.forEachIndexed { index, title ->
            secondaryItems.add(
                Item(
                    title,
                    secondaryImages.getResourceId(index, 0),
                    IntRange(1, 25).random().toFloat()
                )
            )
        }

        binding.showMainItems.setOnClickListener {
            firstPicker?.showMainItems()
        }
        binding.showSecondaryItems.setOnClickListener {
            firstPicker?.showSecondaryItems()
        }

        // If picker is already initialized, remove it from its parent and add it to the new container
        firstPicker?.parent?.let {
            (it as ViewGroup).removeView(firstPicker)
            container.addView(firstPicker)
            return
        }

        firstPicker = BubblePicker(this.requireContext(), null)
        firstPicker!!.apply {
            adapter = object : BubblePickerAdapter {

                override val totalItemCount = primaryItems.size + secondaryItems.size
                override val mainItemCount = primaryItems.size
                override val secondaryItemCount = secondaryItems.size

                override fun getMainItem(position: Int): PickerItem {
                    return PickerItem().apply {
                        val mainItem = primaryItems[position]
                        value = mainItem.value
                        title = mainItem.title
                        imgDrawable = ContextCompat.getDrawable(
                            this@DemoSingleFragment.requireContext(),
                            mainItem.imgResId
                        )
                        id = mainItem.imgResId
                    }
                }

                override fun getSecondaryItem(position: Int): PickerItem {
                    return PickerItem().apply {
                        val secondaryItem = secondaryItems[position]
                        value = secondaryItem.value
                        title = secondaryItem.title
                        imgDrawable = ContextCompat.getDrawable(
                            this@DemoSingleFragment.requireContext(),
                            secondaryItem.imgResId
                        )
                        id = secondaryItem.imgResId
                        isSecondary = true
                    }
                }
            }

            container.addView(this)
            swipeMoveSpeed = 1f
            configSpeedMoveOfItem(20f)
            configMargin(0.001f)
            configListenerForBubble(object : BubblePickerListener {
                override fun onBubbleSelected(item: PickerItem) = toast("${item.title} selected")

                override fun onBubbleDeselected(item: PickerItem) =
                    toast("${item.title} deselected")
            })
            setMaxBubbleSize(0.4f)
            setMinBubbleSize(0.1f)
            configHorizontalSwipeOnly(false)
        }
    }

    override fun onResume() {
        super.onResume()
        firstPicker?.onResume()
    }

    override fun onPause() {
        super.onPause()
        firstPicker?.onPause()
    }
    private fun toast(text: String) =
        Toast.makeText(this.requireContext(), text, Toast.LENGTH_SHORT).show()

}