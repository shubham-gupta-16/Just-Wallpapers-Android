package com.shubhamgupta16.wallpaperapp.ui

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.shubhamgupta16.wallpaperapp.R
import com.shubhamgupta16.wallpaperapp.adapters.SingleImageAdapter
import com.shubhamgupta16.wallpaperapp.databinding.ActivityFullWallpaperBinding
import com.shubhamgupta16.wallpaperapp.databinding.SheetLayoutSetOnBinding
import com.shubhamgupta16.wallpaperapp.models.wallpapers.Author
import com.shubhamgupta16.wallpaperapp.models.wallpapers.WallModel
import com.shubhamgupta16.wallpaperapp.models.wallpapers.WallModelListHolder
import com.shubhamgupta16.wallpaperapp.utils.*
import com.shubhamgupta16.wallpaperapp.viewmodels.PagerViewModel
import com.shubhamgupta16.wallpaperapp.viewmodels.live_observer.ListCase
import dagger.hilt.android.AndroidEntryPoint
import jp.wasabeef.glide.transformations.ColorFilterTransformation
import jp.wasabeef.glide.transformations.CropCircleWithBorderTransformation
import jp.wasabeef.glide.transformations.gpu.BrightnessFilterTransformation
import java.util.*


@AndroidEntryPoint
class FullWallpaperActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullWallpaperBinding
    private val viewModel: PagerViewModel by viewModels()
    private var adapter: SingleImageAdapter? = null
    private var screenMeasure: ScreenMeasure? = null
    private var currentPosition = 0
    private fun initScreenMeasure() {
        if (screenMeasure == null) {
            screenMeasure = ScreenMeasure(this)
            binding.viewPager2.pivotY = screenMeasure!!.pivot()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullWallpaperBinding.inflate(layoutInflater)
        fitFullScreen()
        setTransparentStatusBar()
        setContentView(binding.root)

        val navigationHeight = getNavigationBarHeight()
        binding.navigationOverlay.setPadding(0, 0, 0, navigationHeight)
        setupViewPager()

        val position = intent.getIntExtra("position", 0)
        val data: Uri? = intent.data
        when {
            intent.hasExtra("list") -> {
                val wallModelListHolder = intent.getSerializableExtra("list") as WallModelListHolder
                initOnList(wallModelListHolder.list)
                binding.viewPager2.setCurrentItem(position, true)
                renderOtherComponents(position)
            }
            data != null -> {
                processOnUri(data)
                viewModel.fetch()
            }
            else -> {
                Toast.makeText(this, "Internal Error", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        }


        viewModel.listObserver.observe(this) {
            it?.let {
                when (it.case) {
                    ListCase.INITIAL_LOADING -> {
                    }
                    ListCase.UPDATED -> {
                        if (it.at == currentPosition)
                            updateFavButton(viewModel.list[currentPosition].isFav)
                    }
                    ListCase.ADDED_RANGE -> {
                        adapter?.notifyItemRangeInserted(it.from, it.itemCount)
                    }
                    ListCase.REMOVED_RANGE -> {
                        adapter?.notifyItemRangeRemoved(it.from, it.itemCount)
                    }
                    ListCase.NO_CHANGE -> {
                    }
                    ListCase.EMPTY -> {

                    }
                }
            }
        }

        binding.favButton.setOnClickListener {
            viewModel.toggleFav(currentPosition)
        }

        binding.shareButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Checkout this Cool Wallpaper - Wallpaper App")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Checkout this Cool Wallpaper\n ${getString(R.string.SCHEMA)}://${getString(R.string.BASE_URL)}/id/${viewModel.list[currentPosition].wallId}"
                )
            }
            startActivity(Intent.createChooser(intent, "Share via"))
        }

        binding.downloadButton.setOnClickListener {
            if (isHaveWriteExternalStoragePermission()) {
                processDownloadWallpaper()
            } else {

            }

        }

        binding.setWallpaperButton.setOnClickListener {
            val model = viewModel.list[currentPosition]
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val sheetLayout =
                    SheetLayoutSetOnBinding.inflate(layoutInflater)
                val dialog = alertDialog(sheetLayout)
                sheetLayout.onHomeScreenBtn.setOnClickListener {
                    fetchAndApplyWallpaper(model, WallpaperManager.FLAG_SYSTEM)
                    dialog.dismiss()
                }
                sheetLayout.onLockScreenBtn.setOnClickListener {
                    fetchAndApplyWallpaper(model, WallpaperManager.FLAG_LOCK)
                    dialog.dismiss()
                }
                sheetLayout.onBothScreenBtn.setOnClickListener {
                    fetchAndApplyWallpaper(model)
                    dialog.dismiss()
                }
                dialog.show()
            } else {
                fetchAndApplyWallpaper(model)
            }
        }

        binding.viewPager2.reduceDragSensitivity()

        binding.viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                renderOtherComponents(position)
            }
        })
    }

    private fun processDownloadWallpaper(){
        val model = viewModel.list[currentPosition]
        fetchWallpaperBitmap(model) {
            if (it != null)
                if (!saveImageToExternal("app", "${UUID.randomUUID()}", it))
                    Toast.makeText(this, "WTF", Toast.LENGTH_SHORT).show()
        }
        viewModel.downloadWallpaper(model.wallId)
    }


    private fun fetchAndApplyWallpaper(model: WallModel, flag: Int? = null) {
        if (isOrientationLandscape()) {
            Toast.makeText(this, "Set Wallpapers only in Portrait Mode", Toast.LENGTH_SHORT).show()
            return
        }
        fetchWallpaperBitmap(model) {
            if (it != null)
                applyWall(it, it.width, it.height, flag)
        }
        viewModel.downloadWallpaper(model.wallId)
        Toast.makeText(this, "Loading...", Toast.LENGTH_SHORT).show()
        Toast.makeText(this, "Wallpaper Set Successfully!", Toast.LENGTH_SHORT).show()
    }

    private fun processOnUri(uri: Uri) {
        val segments = uri.pathSegments
        Log.d(TAG, "processOnUri: $segments")
        if (segments.contains("id"))
            viewModel.init(segments.last().toInt())
    }

    private fun initOnList(list: List<WallModel>) {
        viewModel.init(
            list,
            intent.getIntExtra("page", 1),
            intent.getIntExtra("lastPage", 1),
            query = intent.getStringExtra("query"),
            color = intent.getStringExtra("color"),
            category = intent.getStringExtra("category"),
        )
    }

    private var thumbEven = false
    var isLastTaskCompleted = true
    val h = Handler(Looper.getMainLooper())
    val runnable = {
        isLastTaskCompleted = true
    }

    private fun renderOtherComponents(position: Int) {
        currentPosition = position
        if (position == viewModel.list.lastIndex)
            viewModel.fetch()
        val wallModel = viewModel.list[position]
        updateAuthor(wallModel.author)
        updateFavButton(wallModel.isFav)

        val multiTransformation = MultiTransformation(
            RotationTransform((wallModel.rotation ?: 0f).toFloat()),
            FastBlurTransform(),
            BrightnessFilterTransformation(0.2f),
            ColorFilterTransformation(darkenColor(wallModel.color, 156))
        )



        Glide.with(this@FullWallpaperActivity).load(wallModel.urls.small)
            .transform(multiTransformation).addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    if (isLastTaskCompleted) {
                        thumbEven = !thumbEven
                        binding.thumbViewEven.fadeVisibility(
                            if (thumbEven) View.INVISIBLE else View.VISIBLE,
                            400
                        )
                    }
                    val status = isLastTaskCompleted
                    isLastTaskCompleted = false
                    Log.d("TAG", "onResourceReady: $status")
                    h.removeCallbacks(runnable)
                    h.postDelayed(runnable, 400)
                    return !status
                }
            })
            .into(if (thumbEven) binding.thumbViewEven else binding.thumbView)

    }

    private fun updateFavButton(fav: Boolean) {
        binding.favButton.setImageResource(
            if (fav) R.drawable.ic_baseline_favorite_24
            else R.drawable.ic_baseline_favorite_border_24
        )
    }

    private fun setupViewPager() {
        val h = Handler(Looper.getMainLooper())
        val r = Runnable {
            if (!isZoom) animateZoom() else animateZoomOut()
        }
        adapter = SingleImageAdapter(this, viewModel.list) {
            if (isOrientationLandscape()) return@SingleImageAdapter
            if (!isZoom) hideSystemUI() else showSystemUI()
            h.post(r)
        }
        binding.viewPager2.adapter = adapter
        binding.viewPager2.apply3DSwiper()
        /*(binding.viewPager2.getChildAt(0)as RecyclerView).apply {
            setItemViewCacheSize(50)
            setHasFixedSize(true)
        }*/
    }

    private var isZoom = false
    private fun animateZoom() {
        initScreenMeasure()
        val interpolatorCompat = PathInterpolatorCompat.create(0.4f,-0.03f,.26f,1.1f)
//        val interpolatorCompat = PathInterpolatorCompat.create(0.32f,0.41f,1f,.26f)
        isZoom = true
        binding.viewPager2.animate().scaleX(screenMeasure!!.scale()).scaleY(screenMeasure!!.scale())
            .setInterpolator(interpolatorCompat)
            .setDuration(200).start()
        binding.viewPager2.isUserInputEnabled = false
        animateActionCard(false)
    }

    private fun animateZoomOut() {
        initScreenMeasure()
        isZoom = false
        binding.viewPager2.animate().scaleX(1f).scaleY(1f).setDuration(200)
            .setInterpolator(DecelerateInterpolator())
            .start()
        binding.viewPager2.isUserInputEnabled = true
        animateActionCard(true)
    }

    private var isActionCardVisible = true

    private fun animateActionCard(isVisible: Boolean) {
        val y = if (isVisible) 0f else screenMeasure!!.actionCollapsedY()
        if (isVisible != isActionCardVisible) {
            val animator = binding.actionCard.animate().translationY(y).setDuration(200)
            animator.interpolator = DecelerateInterpolator()
            animator.start()
            isActionCardVisible = isVisible
        }
    }

    override fun onBackPressed() {
        if (viewModel.isShowingSharedImage()) {
            val intent = Intent(this, SplashActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        } else
            super.onBackPressed()

    }

    private fun updateAuthor(author: Author?) {
        if (author == null) {
            binding.authorContainer.visibility = View.INVISIBLE
            return
        }
        binding.authorContainer.visibility = View.VISIBLE
        binding.authorName.text = author.name
        Glide.with(this).load(author.image).transform(CropCircleWithBorderTransformation())
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.authorProfile)
    }

    companion object {
        private const val TAG = "FullWallpaperActivity"
        fun getLaunchingIntent(
            context: Context,
            listHolder: WallModelListHolder,
            position: Int,
            page: Int,
            lastPage: Int,
            query: String? = null,
            color: String? = null,
            category: String? = null
        ): Intent {
            val intent = Intent(context, FullWallpaperActivity::class.java)
            intent.putExtra("list", listHolder)
            intent.putExtra("position", position)
            intent.putExtra("page", page)
            intent.putExtra("lastPage", lastPage)
            intent.putExtra("query", query)
            intent.putExtra("color", color)
            intent.putExtra("category", category)
            return intent
        }
    }
}