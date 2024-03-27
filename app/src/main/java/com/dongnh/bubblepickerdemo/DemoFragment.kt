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

    lateinit var mainImages: TypedArray
    lateinit var secondaryImages: TypedArray
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
        //configView()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configView()
    }

    private fun configView() {
        val mainTitles = resources.getStringArray(R.array.main_countries)
        val secondaryTitles = resources.getStringArray(R.array.secondary_countries)
        mainImages = resources.obtainTypedArray(R.array.main_images)
        secondaryImages = resources.obtainTypedArray(R.array.secondary_images)
        binding.showMainItems.setOnClickListener {
            picker.showMainItems()
        }
        binding.showSecondaryItems.setOnClickListener {
            picker.showSecondaryItems()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            picker = BubblePicker(this.requireContext(), null)
            picker.adapter = object : BubblePickerAdapter {

                override val totalCount = mainTitles.size + secondaryTitles.size
                override val mainCount = mainTitles.size
                override val secondaryCount = secondaryTitles.size

                override fun getMainItem(position: Int): PickerItem {
                    return PickerItem().apply {
                        radius = 30f + (position % 16 * 5)
                        title = mainTitles[position % mainTitles.size]
                        imgDrawable = ContextCompat.getDrawable(
                            this@DemoFragment.requireContext(),
                            mainImages.getResourceId(position % 16, 0)
                        )
                        id = position
                    }
                }

                override fun getSecondaryItem(position: Int): PickerItem {
                    return PickerItem().apply {
                        radius = 30f + (position % 16 * 5)
                        title = secondaryTitles[position % secondaryTitles.size]
                        imgDrawable = ContextCompat.getDrawable(
                            this@DemoFragment.requireContext(),
                            secondaryImages.getResourceId(position % 16, 0)
                        )
                        id = mainTitles.size + position
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
            picker.configHorizontalSwipeOnly(true)
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
        mainImages.resources
    }


    private fun toast(text: String) =
        Toast.makeText(this.requireContext(), text, Toast.LENGTH_SHORT).show()

}