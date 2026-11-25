package com.exmore.app.v11.ui

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.PageRange
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Android's native print chooser, backed by the same offline A4 PDF renderer. */
internal object NeoDocumentPrinter {
    fun print(context:Context,noteId:String,title:String,source:String,
        pageBorderStyle:String?=null) {
        val manager=context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val documentName=title.ifBlank{"ExMore Note"}.take(60)
        manager.print(documentName,object:PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes:PrintAttributes?,
                newAttributes:PrintAttributes,
                cancellationSignal:CancellationSignal,
                callback:LayoutResultCallback,
                extras:Bundle?
            ) {
                if(cancellationSignal.isCanceled) {
                    callback.onLayoutCancelled();return
                }
                val info=PrintDocumentInfo.Builder(documentName+".pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN).build()
                callback.onLayoutFinished(info,true)
            }

            override fun onWrite(
                pages:Array<out PageRange>,
                destination:ParcelFileDescriptor,
                cancellationSignal:CancellationSignal,
                callback:WriteResultCallback
            ) {
                CoroutineScope(Dispatchers.IO).launch {
                    if(cancellationSignal.isCanceled) {
                        callback.onWriteCancelled();return@launch
                    }
                    try {
                        FileOutputStream(destination.fileDescriptor).use{out ->
                            NeoDocumentPdf.write(context,noteId,title,source,out,pageBorderStyle)
                        }
                        if(cancellationSignal.isCanceled)callback.onWriteCancelled()
                        else callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch(e:Exception) {
                        callback.onWriteFailed(e.message ?: "Cannot print note")
                    }
                }
            }
        },PrintAttributes.Builder().setMediaSize(
            PrintAttributes.MediaSize.ISO_A4).setMinMargins(
            PrintAttributes.Margins.NO_MARGINS).build())
    }
}
