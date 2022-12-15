package com.igalata.bubblepickerdemo

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
import com.igalata.bubblepicker.BubblePickerListener
import com.igalata.bubblepicker.adapter.BubblePickerAdapter
import com.igalata.bubblepicker.model.BubbleGradient
import com.igalata.bubblepicker.model.PickerItem
import com.igalata.bubblepicker.rendering.BubblePicker
import com.igalata.bubblepickerdemo.databinding.FragmentDemoBinding

/**
 * Project : Bubble-Picker
 * Created by DongNH on 15/12/2022.
 * Email : hoaidongit5@gmail.com or hoaidongit5@dnkinno.com.
 * Phone : +84397199197.
 */
class DemoFragment : Fragment() {

    lateinit var binding: FragmentDemoBinding

    lateinit var images: TypedArray
    lateinit var colors: TypedArray
    lateinit var picker: BubblePicker

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
        val titles = resources.getStringArray(R.array.countries)
        colors = resources.obtainTypedArray(R.array.colors)
        images = resources.obtainTypedArray(R.array.images)

        Handler(Looper.getMainLooper()).postDelayed({
            picker = BubblePicker(this.requireContext(), null)
            picker.adapter = object : BubblePickerAdapter {

                override val totalCount = titles.size

                override fun getItem(position: Int): PickerItem {
                    return PickerItem().apply {
                        title = titles[position]
                        gradient = BubbleGradient(
                            colors.getColor((position * 2) % 8, 0),
                            colors.getColor((position * 2) % 8 + 1, 0), BubbleGradient.VERTICAL
                        )
                        typeface = mediumTypeface
                        textColor = ContextCompat.getColor(
                            this@DemoFragment.requireContext(),
                            android.R.color.white
                        )
                        imgDrawable = ContextCompat.getDrawable(
                            this@DemoFragment.requireContext(),
                            images.getResourceId(position, 0)
                        )
                    }
                }
            }

            binding.root.addView(picker)
            picker.configBubbleSize(100)
            picker.swipeMoveSpeed = 1f
            picker.configSpeedMoveOfItem(20f)
            picker.configAlwaysSelected(false)
            picker.configMargin(0.001f)
            picker.listener = object : BubblePickerListener {
                override fun onBubbleSelected(item: PickerItem) = toast("${item.title} selected")

                override fun onBubbleDeselected(item: PickerItem) =
                    toast("${item.title} deselected")
            }
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