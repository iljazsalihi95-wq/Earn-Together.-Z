package com.weearntogether.play

import android.content.Context

class PlayHistory(context: Context) {
    private val prefs = context.getSharedPreferences("wet_play_history", Context.MODE_PRIVATE)

    val lastGame: String? get() = prefs.getString("last_game", null)
    val lastPlayedAt: Long get() = prefs.getLong("last_played_at", 0L)

    fun mark(game: String) {
        prefs.edit().putString("last_game", game).putLong("last_played_at", System.currentTimeMillis()).apply()
    }
}
