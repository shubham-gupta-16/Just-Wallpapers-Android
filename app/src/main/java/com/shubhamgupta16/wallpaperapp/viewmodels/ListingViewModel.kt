package com.shubhamgupta16.wallpaperapp.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shubhamgupta16.wallpaperapp.models.app.WallModel
import com.shubhamgupta16.wallpaperapp.network.ApiService
import com.shubhamgupta16.wallpaperapp.network.ListCase
import com.shubhamgupta16.wallpaperapp.network.ListObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ListingViewModel : ViewModel() {
    private val apiService = ApiService.getInstance()

    private val _listObserver = MutableLiveData<ListObserver>()
    val listObserver: LiveData<ListObserver> = _listObserver

    private val _list = ArrayList<WallModel?>()
    val list: List<WallModel?> = _list

    private var _page = 1
    private var _lastPage = 1
    private var _query: String? = null
    private var _category: String? = null
    private var _color: String? = null

    val page get() = _page
    val lastPage get() = _lastPage
    val query get() = _query
    val category get() = _category
    val color get() = _color

    fun fetch() {
        if (_page > _lastPage) return
        Log.d(TAG, "fetch: $_lastPage, $_page")
        if (_page == 1) {
            val size = _list.size
            _list.clear()
            _listObserver.value = ListObserver(ListCase.REMOVED, 0, size)
        }
        viewModelScope.launch(Dispatchers.IO) {
            val response =
                apiService.getWalls(page = _page, s = _query, category = _category, color = _color)

            if (response.isSuccessful) {
                response.body()?.let {
                    _lastPage = it.lastPage
                    val size = _list.size
                    if (_list.isNotEmpty())
                        _list.removeAt(_list.lastIndex)
                    _list.addAll(it.data)
                    if (_lastPage > _page)
                        _list.add(null)
                    _page++
                    if (_list.isNotEmpty())
                        _listObserver.postValue(ListObserver(ListCase.UPDATED, at = size - 1))
                    _listObserver.postValue(ListObserver(ListCase.ADDED, from = size, itemCount = _list.size))
                }
            } else
                _listObserver.postValue(ListObserver(ListCase.NO_CHANGE))
        }
    }

    fun setQuery(query: String?) {
        _page = 1
        this._query = query
    }

    fun setCategory(category: String?) {
        _page = 1
        this._category = category
    }

    fun setColor(color: String?) {
        _page = 1
        this._color = color
    }

    companion object {
        private const val TAG = "ListingViewModel"

        private var instance : ListingViewModel? = null
        fun getInstance() =
            instance ?: synchronized(ListingViewModel::class.java){
                instance ?: ListingViewModel().also { instance = it }
            }
    }
}