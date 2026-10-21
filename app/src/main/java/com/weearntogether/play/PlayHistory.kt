package com.weearntogether.play

import android.content.Context

/** Local gameplay history used only for resume and non-monetary mission progress. */
class PlayHistory(context: Context) {
    private val prefs = context.getSharedPreferences("wet_play_history", Context.MODE_PRIVATE)

    val lastGame: String? get() = prefs.getString("last_game", null)
    val lastPlayedAt: Long get() = prefs.getLong("last_played_at", 0L)

    fun plays(game: String): Int = prefs.getInt("plays_$game", 0).coerceAtLeast(0)

    fun mark(game: String) {
        val next = (plays(game) + 1).coerceAtMost(1_000_000)
        prefs.edit()
            .putString("last_game", game)
            .putLong("last_played_at", System.currentTimeMillis())
            .putInt("plays_$game", next)
            .apply()
    }
}
