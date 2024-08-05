package com.microsoft.notes.richtext.render

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import androidx.annotation.AnyRes

@Throws(Resources.NotFoundException::class)
fun Context.getUriToResource(@AnyRes resId: Int): Uri {
    val res = this.resources
    val resUri = Uri.parse(
        ContentResolver.SCHEME_ANDROID_RESOURCE +
            "://" + res.getResourcePackageName(resId) +
            '/' + res.getResourceTypeName(resId) +
            '/' + res.getResourceEntryName(resId)
    )
    return resUri
}
