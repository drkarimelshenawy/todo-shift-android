package com.exmore.app.v11

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

/**
 * Bounded still-image importer: preserve PNG transparency and JPEG format,
 * but reduce pixel dimensions and JPEG quality until an image fits the 4 MiB
 * per-attachment backup limit. Never load the full-resolution bitmap blindly.
 */
internal object NeoImageOptimizer {
    const val MAX_INCOMING_IMAGE = 16*1024*1024
    const val MAX_EDGE = 2048

    fun optimize(source:ByteArray,mime:String,maxBytes:Int=NeoAttachmentStore.MAX_FILE):ByteArray {
        require(mime=="image/jpeg" || mime=="image/png")
        require(source.size<=MAX_INCOMING_IMAGE)
        if(source.size<=maxBytes) return source

        val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true}
        BitmapFactory.decodeByteArray(source,0,source.size,bounds)
        require(bounds.outWidth in 1..20_000 && bounds.outHeight in 1..20_000) {
            "Invalid image dimensions"
        }
        var sample=1
        while(bounds.outWidth/sample>MAX_EDGE*2 || bounds.outHeight/sample>MAX_EDGE*2)
            sample*=2
        var working=BitmapFactory.decodeByteArray(source,0,source.size,
            BitmapFactory.Options().apply {inSampleSize=sample;inPreferredConfig=Bitmap.Config.ARGB_8888})
            ?: error("Could not decode the large image")
        try {
            for(edge in listOf(MAX_EDGE,1600,1280,1024,800)) {
                val scale=minOf(1.0f,edge.toFloat()/maxOf(working.width,working.height))
                if(scale<1f) {
                    val width=maxOf(1,(working.width*scale).toInt())
                    val height=maxOf(1,(working.height*scale).toInt())
                    val resized=Bitmap.createScaledBitmap(working,width,height,true)
                    if(resized!==working) {working.recycle();working=resized}
                }
                val qualities=if(mime=="image/jpeg") listOf(85,70,55) else listOf(100)
                for(quality in qualities) {
                    val output=ByteArrayOutputStream()
                    val format=if(mime=="image/jpeg") Bitmap.CompressFormat.JPEG
                        else Bitmap.CompressFormat.PNG
                    check(working.compress(format,quality,output)) {"Image compression failed"}
                    if(output.size()<=maxBytes) return output.toByteArray()
                }
            }
        } finally {working.recycle()}
        error("Image cannot fit the 4 MiB safety limit; choose a smaller photo")
    }
}
