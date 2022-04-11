package com.shubhamgupta16.wallpaperapp.models.wallpapers


import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class FeaturedModel(
    @SerializedName("data")
    val data: WallModel,
    @SerializedName("sub_title")
    val subTitle: String?,
    @SerializedName("title")
    val title: String?
)