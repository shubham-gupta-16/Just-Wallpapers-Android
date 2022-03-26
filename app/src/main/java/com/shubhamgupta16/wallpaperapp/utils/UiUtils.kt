package com.shubhamgupta16.wallpaperapp.utils

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.TransitionDrawable
import android.os.Handler
import android.os.Looper
import android.transition.Fade
import android.transition.Transition
import android.transition.TransitionManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.shubhamgupta16.wallpaperapp.R
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

val Int.dp get() = (this / (Resources.getSystem().displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()
val Float.dp get() = this / (Resources.getSystem().displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)

val Int.px get() :Int = (this * Resources.getSystem().displayMetrics.density).roundToInt()
val Float.px get() = this * Resources.getSystem().displayMetrics.density

fun View.fadeVisibility(visibility: Int, duration: Long = 400) {
    val transition: Transition = Fade()
    transition.duration = duration
    transition.addTarget(this)
    TransitionManager.beginDelayedTransition(this.parent as ViewGroup, transition)
    this.visibility = visibility
}
fun Activity.closeKeyboard() {
    currentFocus?.let { view ->
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

fun RequestBuilder<Bitmap>.addBitmapListener(
    listener: (isReady: Boolean, resource: Bitmap?, e: GlideException?) -> Unit
): RequestBuilder<Bitmap?> {
    return addListener(object : RequestListener<Bitmap> {
        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<Bitmap>?,
            isFirstResource: Boolean
        ): Boolean {
            listener(false, null, e)
            return false
        }

        override fun onResourceReady(
            resource: Bitmap?,
            model: Any?,
            target: Target<Bitmap>?,
            dataSource: DataSource?,
            isFirstResource: Boolean
        ): Boolean {
            listener(true, resource, null)
            return false
        }

    })
}

fun ViewPager2.reduceDragSensitivity(f: Int = 4) {
    val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
    recyclerViewField.isAccessible = true
    val recyclerView = recyclerViewField.get(this) as RecyclerView

    val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
    touchSlopField.isAccessible = true
    val touchSlop = touchSlopField.get(recyclerView) as Int
    touchSlopField.set(recyclerView, touchSlop*f)       // "8" was obtained experimentally
}

fun getBitmapFromView(view: View): Bitmap? {
    val bitmap =
        Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    view.draw(canvas)
    return bitmap
}

fun darkenColor(stringColor: String, alpha:Int = 24): Int {
    val color = Color.parseColor(stringColor)
    return ColorUtils.setAlphaComponent(Color.HSVToColor(FloatArray(3).apply {
        Color.colorToHSV(color, this)
        this[1] *= 0.6f
        this[2] = if (this[2] > 0.7f) 0.6f else 0.35f
    }), alpha)
}

fun ImageView.fadeImage(bitmap: Bitmap?) {
    val td = TransitionDrawable(
        arrayOf(
            drawable ?: ColorDrawable(Color.TRANSPARENT),
            BitmapDrawable(resources, bitmap)
        )
    )
    setImageDrawable(td)
    Handler(Looper.getMainLooper()).post {
        td.startTransition(200)
    }
}
fun ImageView.fadeImage(color: Int) {
    val td = TransitionDrawable(
        arrayOf(
            drawable ?: ColorDrawable(Color.TRANSPARENT),
            ColorDrawable(color)
        )
    )
    setImageDrawable(td)
    Handler(Looper.getMainLooper()).post {
        td.startTransition(200)
    }
}
fun Activity.fitFullScreen(){
    WindowCompat.setDecorFitsSystemWindows(window, false)
}

fun Activity.setTransparentStatusBar() {
    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
    window.statusBarColor = Color.TRANSPARENT
    window.navigationBarColor = Color.parseColor("#01ffffff")
}
fun Activity.setNormalStatusBar() {
    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
    window.statusBarColor = ContextCompat.getColor(this, R.color.status_bar)
//    window.navigationBarColor = Color.BLACK
}

fun Activity.hideSystemUI() {
    WindowInsetsControllerCompat(window, window.decorView).let { controller ->
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
    }
}

fun Activity.showSystemUI() {
    WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
}

fun Context.getNavigationBarHeight(): Int {
    val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        resources.getDimensionPixelSize(resourceId)
    } else
        0
}
