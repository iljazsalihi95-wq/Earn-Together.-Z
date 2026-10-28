package com.weearntogether.play

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.util.Locale

/** Primary bank is the existing Sheet. Cached snapshot is explicitly labeled offline. */
class QuizBank(private val context:Context) {
    companion object { const val SHEET_ID="1UZjTbjKVszSL6mfcQJwQKoIlyiqJ_liZxTQ18l7M_Xk" }
    private val prefs=context.getSharedPreferences("quiz_arena_bank",Context.MODE_PRIVATE)
    data class Loaded(val questions:List<QuizArenaActivity.Question>,val live:Boolean,val total:Int)
    fun cached():Loaded {
        val rows=runCatching { JSONArray(prefs.getString("rows",null)) }.getOrElse { JSONObject(context.assets.open("quiz-arena/sheet-snapshot.json").bufferedReader().use { it.readText() }).getJSONArray("rows") }
        return Loaded(parse(rows),false,rows.length())
    }
    fun refresh():Loaded {
        val rows=fetchRows("Pyetjet");runCatching{fetchRows("Arena - Pyetjet e tua")}.onSuccess{extra->for(i in 0 until extra.length()){val row=extra.getJSONArray(i);if(row.optString(0)!="ID")rows.put(row)}}
        val questions=parse(rows);require(questions.isNotEmpty())
        prefs.edit().putString("rows",rows.toString()).putLong("fetched",System.currentTimeMillis()).apply()
        return Loaded(questions,true,rows.length())
    }
    private fun fetchRows(sheet:String):JSONArray {
        val url=URL("https://docs.google.com/spreadsheets/d/$SHEET_ID/gviz/tq?tqx=out:json&range=A1:N300&sheet="+URLEncoder.encode(sheet,"UTF-8"))
        val c=url.openConnection() as HttpURLConnection
        try { c.connectTimeout=10000;c.readTimeout=15000;c.useCaches=false
            val text=c.inputStream.bufferedReader().use { it.readText() };val json=JSONObject(text.substring(text.indexOf('{'),text.lastIndexOf('}')+1));require(json.optString("status")=="ok")
            val table=json.getJSONObject("table").getJSONArray("rows");val rows=JSONArray()
            for(i in 0 until table.length()){val cells=table.getJSONObject(i).getJSONArray("c");val row=JSONArray();for(j in 0 until cells.length())row.put(cells.optJSONObject(j)?.opt("v")?:"");rows.put(row)}
            return rows
        } finally { c.disconnect() }
    }
    fun parse(rows:JSONArray):List<QuizArenaActivity.Question> {
        val out=mutableListOf<QuizArenaActivity.Question>()
        for(i in 0 until rows.length()) { val r=rows.getJSONArray(i);if(r.optString(1).uppercase(Locale.ROOT)!="PO")continue
            if(r.optString(13).uppercase(Locale.ROOT).contains("RISHIKIM"))continue
            val choices=(4..8).map { r.optString(it).takeUnless { v->v=="null" }?:"" };val populated=choices.withIndex().filter { it.value.isNotBlank() };val oldCorrect=r.optString(9).trim().uppercase(Locale.ROOT).firstOrNull()?.minus('A') ?: -1
            val correct=populated.indexOfFirst { it.index==oldCorrect };val prompt=r.optString(3)
            if(populated.size !in 2..5||correct<0||prompt.isBlank())continue
            val id=r.opt(0).toString().removeSuffix(".0")
            out+=QuizArenaActivity.Question(r.optString(2).uppercase(Locale.ROOT),prompt,populated.map { it.value },correct,r.optString(10),"sheet-$id",r.optString(11))
        }
        return out.distinctBy { it.id }
    }
}
