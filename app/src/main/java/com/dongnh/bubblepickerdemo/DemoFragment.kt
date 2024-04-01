package com.dongnh.bubblepickerdemo

import android.annotation.SuppressLint
import android.content.res.TypedArray
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.dongnh.bubblepickerdemo.databinding.FragmentDemoBinding

/**
 * Project : Bubble-Picker
 * Created by DongNH on 15/12/2022.
 * Email : hoaidongit5@gmail.com or hoaidongit5@dnkinno.com.
 * Phone : +84397199197.
 */
class DemoFragment : Fragment() {

    private lateinit var binding: FragmentDemoBinding

    data class Item(
        val title: String,
        val imgDrawable: Int,
        val value: Float
    )

    lateinit var primaryItems: MutableList<Item>
    lateinit var secondaryItems: MutableList<Item>
    private lateinit var colors: TypedArray
    private lateinit var picker: BubblePicker

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDemoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configView()
    }

    @SuppressLint("Recycle")
    private fun configView() {
        val primaryTitles = resources.getStringArray(R.array.countries_primary)
        val primaryImages = resources.obtainTypedArray(R.array.images_primary)
        primaryItems = mutableListOf()
        primaryTitles.forEachIndexed { index, title ->
            primaryItems.add(Item(title, primaryImages.getResourceId(index, 0), IntRange(50, 150).random().toFloat()))
        }

        val secondaryTitles = resources.getStringArray(R.array.countries_secondary)
        val secondaryImages = resources.obtainTypedArray(R.array.images_secondary)
        secondaryItems = mutableListOf()
        secondaryTitles.forEachIndexed { index, title ->
            secondaryItems.add(Item(title, secondaryImages.getResourceId(index, 0), IntRange(1, 25).random().toFloat()))
        }

        binding.showMainItems.setOnClickListener {
            picker.showMainItems()
        }
        binding.showSecondaryItems.setOnClickListener {
            picker.showSecondaryItems()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            picker = BubblePicker(this.requireContext(), null)
            picker.adapter = object : BubblePickerAdapter {

                override val totalItemCount = primaryItems.size + secondaryItems.size
                override val mainItemCount = primaryItems.size
                override val secondaryItemCount = secondaryItems.size

                override fun getMainItem(position: Int): PickerItem {
                    return PickerItem().apply {
                        val mainItem = primaryItems[position]
                        value = mainItem.value
                        title = mainItem.title
                        imgDrawable = ContextCompat.getDrawable(
                            this@DemoFragment.requireContext(),
                            mainItem.imgDrawable
                        )
                    }
                }

                override fun getSecondaryItem(position: Int): PickerItem {
                    return PickerItem().apply {
                        val secondaryItem = secondaryItems[position]
                        value = secondaryItem.value
                        title = secondaryItem.title
                        imgDrawable = ContextCompat.getDrawable(
                            this@DemoFragment.requireContext(),
                            secondaryItem.imgDrawable
                        )
                    }
                }
            }

            binding.pickerContainer.addView(picker)
            picker.configCenterImmediately(true)
            picker.swipeMoveSpeed = 1f
            picker.configSpeedMoveOfItem(20f)
            picker.configMargin(0.001f)
            picker.configListenerForBubble(object : BubblePickerListener {
                override fun onBubbleSelected(item: PickerItem) = toast("${item.title} selected")

                override fun onBubbleDeselected(item: PickerItem) = toast("${item.title} deselected")
            })
            picker.setMaxBubbleSize(0.4f)
            picker.setMinBubbleSize(0.1f)
            picker.configHorizontalSwipeOnly(false)
        }, 300)
    }

    override fun onResume() {
        super.onResume()
        if (::picker.isInitialized) {
            picker.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::picker.isInitialized) {
            picker.onPause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        colors.resources
    }


    private fun toast(text: String) =
        Toast.makeText(this.requireContext(), text, Toast.LENGTH_SHORT).show()

}