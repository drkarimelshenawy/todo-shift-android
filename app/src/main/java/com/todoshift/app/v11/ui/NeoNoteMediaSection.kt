package com.todoshift.app.v11.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.todoshift.app.v11.NeoAttachmentStore
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Media lives in the existing bounded, backup-aware private attachment store.
 * No external URI is persisted and the regular rich-note document stays intact.
 */
@Composable
internal fun NeoNoteMediaSection(
    noteId: String,
    editable: Boolean,
    onChanged: () -> Unit,
    onError: (String) -> Unit
) {
    val context=LocalContext.current
    val store=remember(context) {NeoAttachmentStore(context.applicationContext)}
    val scope=rememberCoroutineScope()
    var files by remember(noteId) {mutableStateOf<List<NeoAttachmentStore.Attachment>>(emptyList())}
    var revision by remember(noteId) {mutableIntStateOf(0)}
    var busy by remember {mutableStateOf(false)}
    var deleting by remember {mutableStateOf<NeoAttachmentStore.Attachment?>(null)}
    val player=remember {mutableStateOf<MediaPlayer?>(null)}
    var playingName by remember {mutableStateOf<String?>(null)}
    val recorder=remember {mutableStateOf<MediaRecorder?>(null)}
    val voiceTemp=remember {mutableStateOf<File?>(null)}
    var recording by remember {mutableStateOf(false)}
    val cameraTemp=remember {mutableStateOf<File?>(null)}
    val lifecycle=LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(noteId,revision) {
        try { files=withContext(Dispatchers.IO) {store.list().filter{it.owner==noteId}} }
        catch(e:Exception) {onError(e.message ?: "Unable to load attachments")}
    }

    fun importFile(uri:Uri,temporary:File?=null) {
        if(busy) {temporary?.delete();return}
        busy=true
        scope.launch {
            try {
                withContext(Dispatchers.IO) {store.import(noteId,uri)}
                revision++
                onChanged()
            } catch(e:Exception) {onError(e.message ?: "Cannot import media")}
            finally {
                withContext(Dispatchers.IO) {temporary?.delete()}
                busy=false
            }
        }
    }

    fun stopPlayer() {
        player.value?.runCatching {stop();release()}
        player.value=null;playingName=null
    }
    fun discardRecording() {
        val active=recorder.value
        recorder.value=null
        recording=false
        active?.runCatching {reset();release()}
        voiceTemp.value?.delete()
        voiceTemp.value=null
    }
    fun finishRecording() {
        val active=recorder.value ?: return
        recorder.value=null
        recording=false
        val file=voiceTemp.value
        voiceTemp.value=null
        try {
            active.stop()
            active.release()
            if(file!=null && file.length()>0) {
                importFile(FileProvider.getUriForFile(context,
                    context.packageName+".attachments",file),file)
            } else {
                file?.delete();onError("Recording has no audio")
            }
        } catch(e:Exception) {
            active.runCatching {reset();release()}
            file?.delete()
            onError(e.message ?: "Audio capture failed")
        }
    }
    fun beginRecording() {
        if(!editable||busy||recording) return
        val target=File(context.filesDir,
            "attachments/neo-voice-draft-"+UUID.randomUUID()+".m4a")
        try {
            check(target.parentFile?.isDirectory==true || target.parentFile?.mkdirs()==true)
            @Suppress("DEPRECATION")
            val active=if(Build.VERSION.SDK_INT>=31) MediaRecorder(context)
                else MediaRecorder()
            active.setAudioSource(MediaRecorder.AudioSource.MIC)
            active.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            active.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            active.setAudioEncodingBitRate(64_000)
            active.setAudioSamplingRate(44_100)
            active.setMaxDuration(100_000)
            active.setMaxFileSize(NeoAttachmentStore.MAX_FILE.toLong()-4096)
            active.setOutputFile(target.absolutePath)
            active.setOnInfoListener {_,reason,_->
                if(reason==MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
                    reason==MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED)
                    finishRecording()
            }
            active.prepare();active.start()
            recorder.value=active
            voiceTemp.value=target
            recording=true
        } catch(e:Exception) {
            discardRecording();target.delete()
            onError(e.message ?: "Microphone could not start")
        }
    }

    val micPermission=rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) {granted->
        if(granted) beginRecording()
        else onError("Microphone permission denied")
    }
    val picker=rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()) {uri->if(uri!=null) importFile(uri)}
    val camera=rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()) {success->
        val target=cameraTemp.value;cameraTemp.value=null
        if(target!=null) {
            if(success) importFile(FileProvider.getUriForFile(context,
                context.packageName+".attachments",target),target)
            else target.delete()
        }
    }

    DisposableEffect(noteId,lifecycle) {
        val observer=LifecycleEventObserver {_,event->
            if(event==Lifecycle.Event.ON_STOP) {
                stopPlayer()
                // Recording may not continue in the background.
                if(recording) discardRecording()
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            stopPlayer();discardRecording()
            cameraTemp.value?.delete();cameraTemp.value=null
        }
    }

    Column(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=4.dp),
        verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text(neoLabel("الصور والصوت والمرفقات","Images, audio & files")+
            " ("+files.size+")",style=MaterialTheme.typography.titleSmall)
        if(editable) Row(Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            OutlinedButton(enabled=!busy&&!recording,onClick={
                picker.launch(arrayOf("image/png","image/jpeg","application/pdf",
                    "text/plain","audio/mp4","audio/x-m4a"))
            }) {
                Icon(Icons.Rounded.AttachFile,null,Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp));Text(neoLabel("إدراج ملف","Add file"))
            }
            OutlinedButton(enabled=!busy&&!recording,onClick={
                val target=File(context.filesDir,
                    "attachments/neo-camera-draft-"+UUID.randomUUID()+".jpg")
                try {
                    check(target.parentFile?.isDirectory==true ||
                        target.parentFile?.mkdirs()==true)
                    cameraTemp.value=target
                    camera.launch(FileProvider.getUriForFile(context,
                        context.packageName+".attachments",target))
                } catch(e:Exception) {
                    cameraTemp.value=null;target.delete()
                    onError(e.message ?: "Camera unavailable")
                }
            }) {
                Icon(Icons.Rounded.PhotoCamera,null,Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp));Text(neoLabel("كاميرا","Camera"))
            }
            OutlinedButton(enabled=!busy,onClick={
                if(recording) finishRecording()
                else if(ContextCompat.checkSelfPermission(context,
                    Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)
                    beginRecording()
                else micPermission.launch(Manifest.permission.RECORD_AUDIO)
            }) {
                Icon(if(recording) Icons.Rounded.StopCircle else Icons.Rounded.Mic,
                    null,Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if(recording) neoLabel("إيقاف وحفظ","Stop & save")
                    else neoLabel("سجل صوت","Record"))
            }
        }
        if(recording) Text(
            neoLabel("تسجيل صوتي جارٍ • 100 ثانية كحد أقصى",
                "Voice recording • 100 seconds maximum"),
            color=MaterialTheme.colorScheme.error,
            style=MaterialTheme.typography.labelSmall)
        if(files.isEmpty()) Text(neoLabel("لا توجد مرفقات","No attachments"),
            style=MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            files.forEachIndexed {index,file->
                Card(Modifier.width(150.dp)) {
                    Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        if(file.mime.startsWith("image/")) NeoNoteMediaThumbnail(file.name)
                        else Box(Modifier.fillMaxWidth().height(60.dp),
                            contentAlignment=Alignment.Center) {
                            Icon(if(file.mime=="audio/mp4") Icons.Rounded.GraphicEq
                                else Icons.Rounded.Description,null,Modifier.size(28.dp))
                        }
                        Text(when(file.mime) {
                            "image/png","image/jpeg" -> neoLabel("صورة","Image")
                            "audio/mp4" -> neoLabel("تسجيل صوتي","Voice")
                            "application/pdf" -> "PDF"
                            else -> neoLabel("ملف نصي","Text")
                        }+" "+(index+1),
                            maxLines=1,style=MaterialTheme.typography.labelSmall)
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            TextButton(modifier=Modifier.weight(1f),onClick={
                                if(file.mime=="audio/mp4") {
                                    if(playingName==file.name) stopPlayer()
                                    else try {
                                        stopPlayer()
                                        val next=MediaPlayer()
                                        next.setDataSource(context,store.uri(file.name))
                                        next.setOnPreparedListener {it.start()}
                                        next.setOnCompletionListener {
                                            it.release();player.value=null;playingName=null
                                        }
                                        next.setOnErrorListener {bad,_,_->
                                            bad.release();player.value=null;playingName=null
                                            onError("Playback failed");true
                                        }
                                        player.value=next;playingName=file.name
                                        next.prepareAsync()
                                    } catch(e:Exception) {
                                        stopPlayer();onError(e.message ?: "Cannot play audio")
                                    }
                                } else try {
                                    context.startActivity(Intent.createChooser(
                                        Intent(Intent.ACTION_VIEW)
                                            .setDataAndType(store.uri(file.name),file.mime)
                                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                                        null))
                                } catch(e:Exception) {onError(e.message ?: "No viewer available")}
                            }) {Text(if(file.mime=="audio/mp4")
                                (if(playingName==file.name) neoLabel("إيقاف","Stop")
                                else neoLabel("تشغيل","Play"))
                                else neoLabel("فتح","Open"))}
                            if(editable) IconButton(
                                enabled=!busy&&!recording,
                                modifier=Modifier.size(34.dp),
                                onClick={deleting=file}
                            ) {
                                Icon(Icons.Rounded.DeleteOutline,
                                    neoLabel("فصل المرفق","Detach file"),Modifier.size(19.dp))
                            }
                        }
                    }
                }
            }
        }
    }
    deleting?.let {item->
        AlertDialog(onDismissRequest={deleting=null},
            title={Text(neoLabel("إزالة المرفق؟","Detach attachment?"))},
            text={Text(neoLabel(
                "الملف الأصلي سيظل محفوظًا في مساحة التخزين حتى تنظيف الملفات المنفصلة.",
                "The private file is kept until detached-storage cleanup is explicitly requested."))},
            confirmButton={TextButton(enabled=!busy,onClick={
                deleting=null;busy=true
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {store.detach(item.name)}
                        if(playingName==item.name) stopPlayer()
                        revision++;onChanged()
                    } catch(e:Exception) {onError(e.message ?: "Detach failed")}
                    finally {busy=false}
                }
            }) {Text(neoLabel("إزالة الربط","Detach"))}},
            dismissButton={TextButton(onClick={deleting=null}) {
                Text(neoLabel("إلغاء","Cancel"))
            }})
    }
}

@Composable
private fun NeoNoteMediaThumbnail(name:String) {
    val context=LocalContext.current
    var bitmap by remember(name) {mutableStateOf<Bitmap?>(null)}
    LaunchedEffect(name) {
        bitmap=withContext(Dispatchers.IO) {runCatching {
            require(NeoAttachmentStore.safeName(name))
            val path=File(context.filesDir,"attachments/"+name).absolutePath
            val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
            BitmapFactory.decodeFile(path,bounds)
            if(bounds.outWidth<=0||bounds.outHeight<=0) return@runCatching null
            var factor=1
            while(bounds.outWidth/factor>480||bounds.outHeight/factor>480) factor*=2
            BitmapFactory.decodeFile(path,BitmapFactory.Options().apply {
                inSampleSize=factor
                inPreferredConfig=Bitmap.Config.RGB_565
            })
        }.getOrNull()}
    }
    Box(Modifier.fillMaxWidth().height(86.dp),contentAlignment=Alignment.Center) {
        if(bitmap!=null) Image(bitmap!!.asImageBitmap(),
            contentDescription=neoLabel("معاينة صورة","Image preview"),
            modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        else Icon(Icons.Rounded.ImageNotSupported,null)
    }
}
