package com.dongnh.bubblepickerdemo

import android.content.res.TypedArray
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dongnh.bubblepicker.BubblePickerListener
import com.dongnh.bubblepicker.adapter.BubblePickerAdapter
import com.dongnh.bubblepicker.model.BubbleGradient
import com.dongnh.bubblepicker.model.PickerItem
import com.dongnh.bubblepicker.rendering.BubblePicker

/**
 * Created by irinagalata on 1/19/17.
 */
class DemoActivity : AppCompatActivity() {

    private val mediumTypeface by lazy { Typeface.createFromAsset(assets, ROBOTO_MEDIUM) }

    companion object {
        private const val ROBOTO_BOLD = "roboto_bold.ttf"
        private const val ROBOTO_MEDIUM = "roboto_medium.ttf"
        private const val ROBOTO_REGULAR = "roboto_regular.ttf"
    }

    lateinit var images: TypedArray
    lateinit var colors: TypedArray
    lateinit var picker: BubblePicker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)

        val titles = resources.getStringArray(R.array.countries)
        colors = resources.obtainTypedArray(R.array.colors)
        images = resources.obtainTypedArray(R.array.images)

        Handler(Looper.getMainLooper()).postDelayed({
            picker = BubblePicker(this, null)
            picker.adapter = object : BubblePickerAdapter {

                override val totalItemCount = titles.size
                override val mainItemCount = titles.size

                override fun getMainItem(position: Int): PickerItem {
                    return PickerItem().apply {
                        title = titles[position]
                        gradient = BubbleGradient(
                            colors.getColor((position * 2) % 8, 0),
                            colors.getColor((position * 2) % 8 + 1, 0), BubbleGradient.VERTICAL
                        )
                        radius = 50f + (position * 10)
                        typeface = mediumTypeface
                        textColor = ContextCompat.getColor(this@DemoActivity, android.R.color.white)
                        // If you want to use image url, you need using glide load it and pass to this param
                        imgDrawable = ContextCompat.getDrawable(
                            this@DemoActivity,
                            images.getResourceId(position, 0)
                        )
                    }
                }
            }

            setContentView(picker)
            picker.configCenterImmediately(true)
            picker.swipeMoveSpeed = 1f
            picker.configSpeedMoveOfItem(20f)
            picker.configMargin(0.001f)
            picker.configListenerForBubble(object : BubblePickerListener {
                override fun onBubbleSelected(item: PickerItem) = toast("${item.title} selected")

                override fun onBubbleDeselected(item: PickerItem) =
                    toast("${item.title} deselected")
            })
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

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

}