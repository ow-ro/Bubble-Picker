package com.dongnh.bubblepickerdemo

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dongnh.bubblepicker.BubblePickerListener
import com.dongnh.bubblepicker.BubblePickerOnTouchListener
import com.dongnh.bubblepicker.adapter.BubblePickerAdapter
import com.dongnh.bubblepicker.model.PickerItem
import com.dongnh.bubblepicker.physics.Engine
import com.dongnh.bubblepicker.rendering.BubblePicker
import com.dongnh.bubblepickerdemo.databinding.DemoBubbleCellBinding
import com.dongnh.bubblepickerdemo.databinding.DemoEmptyCellBinding
import com.dongnh.bubblepickerdemo.databinding.FragmentDemoBinding

class DemoFragment : Fragment() {

    private lateinit var binding: FragmentDemoBinding

    data class Item(
        val title: String,
        val imgResId: Int,
        val value: Float
    )

    lateinit var primaryItems: MutableList<Item>
    lateinit var secondaryItems: MutableList<Item>
    private var firstPicker: BubblePicker? = null
    private var secondPicker: BubblePicker? = null
    private val deviceWidth: Int by lazy { resources.displayMetrics.widthPixels }
    private val pickerHeight: Int by lazy {
        val density = resources.displayMetrics.density
        (275 * density).toInt()
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
        binding.recyclerView.layoutManager =
            LinearLayoutManager(this.requireContext(), RecyclerView.VERTICAL, false)
        binding.recyclerView.adapter = SimpleAdapter()
        binding.recyclerView.addItemDecoration(DividerItemDecoration(1, Color.BLACK))
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

        firstPicker = BubblePicker(Engine.Mode.MAIN, false, context, null, object : BubblePickerOnTouchListener {
            override fun onTouchUp(event: MotionEvent) {
                binding.recyclerView.suppressLayout(false)
            }

            override fun onTouchDown() {
                binding.recyclerView.suppressLayout(true)
            }
        }).apply {
            // This must be set before the adapter
            setMaxBubbleSize(0.8f)
            setMinBubbleSize(0.1f)
            adapter = object : BubblePickerAdapter {

                override val totalItemCount = primaryItems.size + secondaryItems.size
                override val mainItemCount = primaryItems.size
                override val secondaryItemCount = secondaryItems.size

                override val width = deviceWidth
                override val height = pickerHeight

                override fun getMainItem(position: Int): PickerItem {
                    return PickerItem().apply {
                        val mainItem = primaryItems[position]
                        value = mainItem.value
                        title = mainItem.title
                        imgDrawable = ContextCompat.getDrawable(
                            this@DemoFragment.requireContext(),
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
                            this@DemoFragment.requireContext(),
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
            configHorizontalSwipeOnly(false)
        }
    }


    @SuppressLint("Recycle")
    private fun configSecondView(container: ViewGroup) {
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

        secondPicker?.parent?.let {
            (it as ViewGroup).removeView(secondPicker)
            container.addView(secondPicker)
            return
        }

        secondPicker = BubblePicker(this.requireContext(), null)
        secondPicker!!.apply {
            // This must be set before the adapter
            setMaxBubbleSize(0.8f)
            setMinBubbleSize(0.1f)
            adapter = object : BubblePickerAdapter {

                override val totalItemCount = primaryItems.size
                override val mainItemCount = primaryItems.size

                override val width = deviceWidth
                override val height = pickerHeight

                override fun getMainItem(position: Int): PickerItem {
                    return PickerItem().apply {
                        val mainItem = primaryItems[position]
                        value = mainItem.value
                        title = mainItem.title
                        imgDrawable = ContextCompat.getDrawable(
                            this@DemoFragment.requireContext(),
                            mainItem.imgResId
                        )
                        id = mainItem.imgResId
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
            configHorizontalSwipeOnly(false)
        }
    }

    override fun onResume() {
        super.onResume()
        firstPicker?.onResume()
        secondPicker?.onResume()
    }

    override fun onPause() {
        super.onPause()
        firstPicker?.onPause()
        secondPicker?.onPause()
    }

    inner class DividerItemDecoration(private val dividerHeight: Int, private val dividerColor: Int) : RecyclerView.ItemDecoration() {

        private val paint = Paint()

        init {
            paint.color = dividerColor
        }

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            super.getItemOffsets(outRect, view, parent, state)
            outRect.bottom = dividerHeight
        }

        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val left = parent.paddingLeft
            val right = parent.width - parent.paddingRight

            val childCount = parent.childCount
            for (i in 0 until childCount) {
                val child = parent.getChildAt(i)
                val params = child.layoutParams as RecyclerView.LayoutParams

                val top = child.bottom + params.bottomMargin
                val bottom = top + dividerHeight

                c.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
            }
        }
    }

    inner class SimpleAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        inner class SimpleViewHolder(val binding: DemoEmptyCellBinding) :
            RecyclerView.ViewHolder(binding.root)

        inner class PickerViewHolder(val binding: DemoBubbleCellBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == 0) {
                val binding = DemoBubbleCellBinding.inflate(LayoutInflater.from(parent.context))
                PickerViewHolder(binding)
            } else {
                val binding = DemoEmptyCellBinding.inflate(LayoutInflater.from(parent.context))
                SimpleViewHolder(binding)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (position) {
                0 -> {
                    val pickerHolder = holder as? PickerViewHolder ?: return
                    val container = pickerHolder.binding.pickerContainer
                    configView(container)
                }
                1 -> {
                    val pickerHolder = holder as? PickerViewHolder ?: return
                    val container = pickerHolder.binding.pickerContainer
                    configSecondView(container)
                }
                else -> {
                    val image = (holder as? SimpleViewHolder)?.binding?.img ?: return
                    image.setImageResource((primaryItems + secondaryItems).random().imgResId)
                }
            }
        }

        override fun getItemCount() = 8
        override fun getItemViewType(position: Int) = if (position == 0 || position == 1) 0 else 1
    }

    private fun toast(text: String) =
        Toast.makeText(this.requireContext(), text, Toast.LENGTH_SHORT).show()

}