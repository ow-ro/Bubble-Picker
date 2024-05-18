package com.dongnh.bubblepicker

import android.view.MotionEvent

interface BubblePickerOnTouchListener {
	fun onTouchDown() {}
	fun onTouchMove(event: MotionEvent) {}
	fun onTouchUp(event: MotionEvent) {}
}