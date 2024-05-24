package com.dongnh.bubblepicker.model

import com.example.libavif.AvifLoader

data class Item(
	val title: String,
	val imgResId: Int,
	val frameInfo: List<AvifLoader.FrameInfo>? = null,
	val value: Float
)