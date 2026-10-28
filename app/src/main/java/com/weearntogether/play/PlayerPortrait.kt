package com.weearntogether.play

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView
import android.graphics.drawable.GradientDrawable
import java.io.File

/** User-selected portrait stays on this device; never uploads the photo or its URI. */
object PlayerPortrait {
    const val REQUEST=2418
    private fun file(context:Context):File {
        val uid=AccountStore(context).currentUserId() ?: "guest"
        return File(context.filesDir,"arena_portrait_"+uid.replace(Regex("[^a-zA-Z0-9_-]"),"_")+".webp")
    }
    fun load(context:Context):Bitmap?=runCatching { BitmapFactory.decodeFile(file(context).path) }.getOrNull()
    fun pick(activity:Activity){activity.startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type="image/*";addCategory(Intent.CATEGORY_OPENABLE);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) },REQUEST)}
    fun receive(context:Context,result:Int,data:Intent?):Boolean {
        if(result!=Activity.RESULT_OK)return false
        val uri=data?.data ?: return false
        return runCatching {
            val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
            context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it,null,bounds) }
            var sample=1;while(bounds.outWidth/sample>640||bounds.outHeight/sample>640)sample*=2
            val bitmap=context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply{inSampleSize=sample}) } ?: return false
            file(context).outputStream().use { bitmap.compress(Bitmap.CompressFormat.WEBP,88,it) };bitmap.recycle();true
        }.getOrDefault(false)
    }
    fun image(context:Context):ImageView=ImageView(context).apply { scaleType=ImageView.ScaleType.CENTER_CROP;setImageBitmap(load(context)?:StudioAssets.get(context,"host").let{Bitmap.createBitmap(it,0,0,it.width/4,it.height/2)});background=GradientDrawable().apply {shape=GradientDrawable.OVAL;setColor(0xffdce9ff.toInt())};clipToOutline=true;contentDescription="Fotoja e lojtarit" }
}
