package com.shubhamgupta16.wallpaperapp.utils

import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class PaginationController(
    recyclerView: RecyclerView, manager: RecyclerView.LayoutManager,
   private val listener: () -> Unit
) {

    private var isLoading = false
    private var hasNextPage = false

    fun notifyDataFetched(hasNextPage:Boolean) {
        isLoading = false
        this.hasNextPage = hasNextPage
    }

    init {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val firstVisibleItemPosition:Int = when (manager) {
                    is LinearLayoutManager -> manager.findFirstVisibleItemPosition()
                    is StaggeredGridLayoutManager ->{ manager.findFirstVisibleItemPositions(null).last()}
                    else -> 0
                }
                    if (!isLoading && hasNextPage && manager.childCount + firstVisibleItemPosition >= manager.itemCount) {
                        Log.d("TAG", "onScrolled: onScrolled")
                        isLoading = true
                        listener()
                    }
            }
        })
    }

}