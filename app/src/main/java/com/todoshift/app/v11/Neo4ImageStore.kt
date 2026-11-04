package com.todoshift.app.v11

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.AtomicFile
import java.io.File
import java.util.UUID

object Neo4ImageStore {
    private val cache = android.util.LruCache<String, Bitmap>(1)
    fun load(context: Context, name: String): Bitmap? = synchronized(cache) {
        if (!name.matches(Regex("neo4-theme-[a-zA-Z0-9-]+[.]jpg"))) return@synchronized null
        cache.get(name)?.let { return@synchronized it }
        val path = File(context.filesDir, name).absolutePath
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth !in 1..1600 || bounds.outHeight !in 1..1600) return@synchronized null
        BitmapFactory.decodeFile(path)?.also { cache.put(name, it) }
    }

    /** Call on Dispatchers.IO. Reopens the picker URI; never asks for broad storage access. */
    fun importPhoto(context: Context, uri: Uri): String {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: error("Cannot read image")
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Invalid image" }
        var sample = 1
        while ((maxOf(bounds.outWidth, bounds.outHeight).toLong() + sample - 1) / sample > 1600) sample *= 2
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
            ?: error("Cannot decode image")
        val orientation = runCatching { resolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) } }.getOrNull()
        val matrix = Matrix().apply { when (orientation) {
            2 -> setScale(-1f, 1f); 3 -> setRotate(180f); 4 -> setScale(1f, -1f)
            5 -> { setRotate(90f); postScale(-1f, 1f) }; 6 -> setRotate(90f)
            7 -> { setRotate(270f); postScale(-1f, 1f) }; 8 -> setRotate(270f)
        } }
        val oriented = if (matrix.isIdentity) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val name = "neo4-theme-${UUID.randomUUID()}.jpg"
        val file = AtomicFile(File(context.filesDir, name)); val output = file.startWrite()
        try { check(oriented.compress(Bitmap.CompressFormat.JPEG, 86, output)); file.finishWrite(output) }
        catch (error: Throwable) { file.failWrite(output); throw error }
        finally { if (oriented !== bitmap) oriented.recycle(); bitmap.recycle() }
        return name
    }
}
