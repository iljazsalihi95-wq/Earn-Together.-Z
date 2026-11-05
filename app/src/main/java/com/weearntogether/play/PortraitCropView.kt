package com.weearntogether.play

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.max

/** Local pan/zoom crop. No network, face alteration, or external image service. */
internal class PortraitCropView(context:Context,private val bitmap:Bitmap):View(context){
    private val paint=Paint(3);private var zoom=1f;private var dx=0f;private var dy=0f;private var lastX=0f;private var lastY=0f
    private val scale=ScaleGestureDetector(context,object:ScaleGestureDetector.SimpleOnScaleGestureListener(){override fun onScale(detector:ScaleGestureDetector):Boolean{zoom=(zoom*detector.scaleFactor).coerceIn(1f,5f);clamp();invalidate();return true}})
    private fun base()=max(width.toFloat()/bitmap.width,height.toFloat()/bitmap.height)
    private fun clamp(){val s=base()*zoom;val mx=((bitmap.width*s-width)/2).coerceAtLeast(0f);val my=((bitmap.height*s-height)/2).coerceAtLeast(0f);dx=dx.coerceIn(-mx,mx);dy=dy.coerceIn(-my,my)}
    private fun render(c:Canvas){val s=base()*zoom;c.translate(width/2f+dx,height/2f+dy);c.scale(s,s);c.drawBitmap(bitmap,-bitmap.width/2f,-bitmap.height/2f,paint)}
    override fun onDraw(c:Canvas){super.onDraw(c);clamp();c.save();render(c);c.restore();paint.style=Paint.Style.STROKE;paint.strokeWidth=3f;paint.color=0xffffd782.toInt();c.drawOval(width*.15f,height*.08f,width*.85f,height*.93f,paint);paint.style=Paint.Style.FILL}
    override fun onTouchEvent(e:MotionEvent):Boolean{scale.onTouchEvent(e);when(e.actionMasked){MotionEvent.ACTION_DOWN->{lastX=e.x;lastY=e.y;parent?.requestDisallowInterceptTouchEvent(true)};MotionEvent.ACTION_MOVE->{if(!scale.isInProgress&&e.pointerCount==1){dx+=e.x-lastX;dy+=e.y-lastY;clamp();invalidate()};lastX=e.x;lastY=e.y};MotionEvent.ACTION_UP->performClick()};return true}
    override fun performClick():Boolean{super.performClick();return true}
    fun result():Bitmap{val out=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);val c=Canvas(out);c.scale(512f/width.coerceAtLeast(1),512f/height.coerceAtLeast(1));render(c);return out}
}
