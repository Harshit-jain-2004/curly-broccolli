package com.microsoft.notes.platform.glide

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory
import com.bumptech.glide.module.AppGlideModule
import com.microsoft.notes.utils.utils.Constants

@GlideModule
class NotesGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val notesFolder = context.let { "${context.filesDir}/${Constants.NOTES_FOLDER_NAME}" }

        builder.setDiskCache(
            DiskLruCacheFactory(
                notesFolder,
                Constants.NOTES_GLIDE_CACHE_FOLDER_NAME, 250 * 1024 * 1024 /*250MB*/
            )
        )
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) { }
}
