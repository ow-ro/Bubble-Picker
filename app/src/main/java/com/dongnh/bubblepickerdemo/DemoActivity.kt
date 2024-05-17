package com.dongnh.bubblepickerdemo

import android.content.res.TypedArray
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.dongnh.bubblepicker.BubblePickerListener
import com.dongnh.bubblepicker.adapter.BubblePickerAdapter
import com.dongnh.bubblepicker.model.BubbleGradient
import com.dongnh.bubblepicker.model.PickerItem
import com.dongnh.bubblepicker.rendering.BubblePicker
import com.example.libavif.AvifDrawableTransformation
import com.example.libavif.AvifLoader
import com.example.libavif.Utils.loadAvif
import com.example.libavif.Utils.toAvifSupportedSource
import com.example.libavif.targets.AvifStreamTarget
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.executeAsync
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
    lateinit var picker: BubblePicker
    lateinit var glide: RequestManager

    private suspend fun preloadAvifFrames(avifStreamTarget: AvifStreamTarget) = suspendCoroutine {
        avifStreamTarget.preloadAllFrames { result ->
            it.resume(result)
        }
    }

    private fun prefetchFrames(urls: List<String>, callback: (List<List<AvifLoader.FrameInfo>>) -> Unit) = MainScope().launch {
        val imageData = urls.map { url ->
            async {
                when (val drawable = loadDrawableViaGlide(url)) {
                    is AvifStreamTarget -> preloadAvifFrames(drawable)
                    is BitmapDrawable -> listOf(AvifLoader.FrameInfo(0, Long.MAX_VALUE, drawable.bitmap))
                    else -> null
                }
            }
        }.awaitAll().filterNotNull()

        callback.invoke(imageData)
    }

    private suspend fun loadDrawableViaGlide(url: String) = suspendCoroutine {
        glide.asDrawable()
            .load(url.toAvifSupportedSource)
            .into(object : CustomTarget<Drawable>() {
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    it.resume(null)
                }
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    it.resume(resource)
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)
        glide = Glide.with(this)

        val urls = listOf(
            "https://raw.githubusercontent.com/ow-ro/HostedFiles/main/test-120w.avif",
            "https://raw.githubusercontent.com/ow-ro/HostedFiles/main/walk-small.avif",
            "https://aomedia.org/assets/images/blog/parrot-avif.avif",
            "https://raw.githubusercontent.com/link-u/avif-sample-images/master/kimono.crop.avif",
            "https://emoji.cdn.wemesh.com/emojis/astronaut/128.avif",
            "https://emoji.cdn.wemesh.com/emojis/alien/128.avif",
            "https://emoji.cdn.wemesh.com/emojis/dog/128.avif",
            "https://www.gstatic.com/webp/gallery/1.webp",
            "https://www.gstatic.com/webp/gallery/2.jpg",
            "https://sample-videos.com/img/Sample-png-image-100kb.png"
        )

        prefetchFrames(urls) {
            buildPicker(it)
        }
    }

    private fun buildPicker(frameInfos: List<List<AvifLoader.FrameInfo>>) {
        images = resources.obtainTypedArray(R.array.images)

        Handler(Looper.getMainLooper()).postDelayed({
            picker = BubblePicker(this, null)
            picker.adapter = object : BubblePickerAdapter {

                override val totalItemCount = frameInfos.size
                override val mainItemCount = frameInfos.size

                // nonsense values
                override val width = 300
                override val height = 300

                override fun getMainItem(position: Int): PickerItem {
                    return PickerItem().apply {
                        value = 50f + (position * 10)
                        typeface = mediumTypeface
                        // If you want to use image url, you need using glide load it and pass to this param
                        imgDrawable = ContextCompat.getDrawable(
                            this@DemoActivity,
                            images.getResourceId(position, 0)
                        )
                        animatedFrames = frameInfos[position]
                    }
                }
            }

            setContentView(picker)
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
        images.resources
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

}