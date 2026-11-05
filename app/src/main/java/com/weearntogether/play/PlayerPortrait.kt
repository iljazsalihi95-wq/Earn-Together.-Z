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
            val decoded=context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply{inSampleSize=sample}) } ?: return false
            val orientation=runCatching{context.contentResolver.openInputStream(uri).use{android.media.ExifInterface(it!!).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,android.media.ExifInterface.ORIENTATION_NORMAL)}}.getOrDefault(1)
            val matrix=android.graphics.Matrix().apply{when(orientation){2->setScale(-1f,1f);3->setRotate(180f);4->setScale(1f,-1f);5->{setRotate(90f);postScale(-1f,1f)};6->setRotate(90f);7->{setRotate(270f);postScale(-1f,1f)};8->setRotate(270f)}}
            val bitmap=if(matrix.isIdentity)decoded else Bitmap.createBitmap(decoded,0,0,decoded.width,decoded.height,matrix,true).also{decoded.recycle()}
            file(context).outputStream().use { bitmap.compress(Bitmap.CompressFormat.WEBP,88,it) };bitmap.recycle();true
        }.getOrDefault(false)
    }
    fun adjust(activity:Activity,onSaved:()->Unit){
        val bitmap=load(activity)?:return onSaved()
        val crop=PortraitCropView(activity,bitmap)
        val panel=android.widget.LinearLayout(activity).apply{orientation=1;setPadding(20,12,20,12);addView(android.widget.TextView(activity).apply{text="Lëvize fotografinë dhe zmadhoje me dy gishta. Fytyra ruhet vetëm në telefon.";setPadding(8,8,8,16)});addView(crop,android.widget.LinearLayout.LayoutParams(-1,(activity.resources.displayMetrics.widthPixels*.76f).toInt()))}
        android.app.AlertDialog.Builder(activity).setTitle("PËRSHTAT PORTRETIN").setView(panel).setPositiveButton("RUAJ"){_,_->
            val result=crop.result();file(activity).outputStream().use{result.compress(Bitmap.CompressFormat.WEBP,90,it)};result.recycle();bitmap.recycle();onSaved()
        }.setNegativeButton("MBAJ ORIGJINALIN"){_,_->bitmap.recycle();onSaved()}.setOnCancelListener{bitmap.recycle();onSaved()}.show()
    }
    fun image(context:Context):ImageView=ImageView(context).apply { scaleType=ImageView.ScaleType.CENTER_CROP;setImageBitmap(load(context)?:StudioAssets.get(context,"host").let{Bitmap.createBitmap(it,0,0,it.width/4,it.height/2)});background=GradientDrawable().apply {shape=GradientDrawable.OVAL;setColor(0xffdce9ff.toInt())};clipToOutline=true;contentDescription="Fotoja e lojtarit" }
}
