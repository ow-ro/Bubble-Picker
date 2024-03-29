package com.dongnh.bubblepickerdemo

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

    lateinit var titles: Array<String>
    lateinit var images: TypedArray
    private lateinit var colors: TypedArray
    private lateinit var picker: BubblePicker

    private val mediumTypeface by lazy {
        Typeface.createFromAsset(
            requireActivity().assets,
            ROBOTO_MEDIUM
        )
    }

    companion object {
        private const val ROBOTO_MEDIUM = "roboto_medium.ttf"
    }

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

    private fun configView() {
        titles = resources.getStringArray(R.array.countries)
        images = resources.obtainTypedArray(R.array.images)
        val halfSize = titles.size / 2
        binding.showMainItems.setOnClickListener {
            picker.showMainItems()
        }
        binding.showSecondaryItems.setOnClickListener {
            picker.showSecondaryItems()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            picker = BubblePicker(this.requireContext(), null)
            picker.adapter = object : BubblePickerAdapter {

                override val totalItemCount = titles.size
                override val mainItemCount = halfSize
                override val secondaryItemCount = halfSize

                override fun getMainItem(position: Int): PickerItem {
                    return PickerItem().apply {
                        value = if (position % halfSize == 0) {
                            20f + ((position % halfSize) * 5f)
                        } else {
                            5f + (position % halfSize)
                        }
                        title = titles[position % halfSize]
                        imgDrawable = ContextCompat.getDrawable(
                            this@DemoFragment.requireContext(),
                            images.getResourceId(position % halfSize, 0)
                        )
                        id = position
                    }
                }

                override fun getSecondaryItem(position: Int): PickerItem {
                    val actualPos = position + halfSize
                    return PickerItem().apply {
                        value = if (actualPos % halfSize == 0) {
                            20f + ((actualPos % halfSize) * 5f)
                        } else {
                            5f + actualPos % halfSize
                        }
                        title = titles[position % halfSize + 4]
                        imgDrawable = ContextCompat.getDrawable(
                            this@DemoFragment.requireContext(),
                            images.getResourceId(position % halfSize + 4, 0)
                        )
                        id = halfSize + position
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
        images.resources
    }


    private fun toast(text: String) =
        Toast.makeText(this.requireContext(), text, Toast.LENGTH_SHORT).show()

}