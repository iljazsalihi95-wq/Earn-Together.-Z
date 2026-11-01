package com.weearntogether.play

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/** Existing unscoped saves are retained. Each online player gets an isolated offline cache. */
object ProgressPreferences {
    val gameFiles = linkedMapOf(
        "LazyGarden" to listOf("lazy_garden_progress", "lazy_garden_wallet", "lazy_garden_milestone"),
        "Block Puzzle" to listOf("block_puzzle", "block_puzzle_hd2d"),
        "Bubble Shooter" to listOf("bubble_shooter", "bubble_shooter_hd2d"),
        "Blade Quest" to listOf("knife_hit")
    )
    private val extras = listOf("wet_loyalty_progress", "wet_play_history", "color_lines", "neon_stack", "merge2048")
    fun user(context: Context): String? = context.getSharedPreferences("wet_cloud_session", Context.MODE_PRIVATE).getString("user_id", null)
    fun open(context: Context, name: String): SharedPreferences {
        val user = user(context)
        val target = forUser(context, name, user)
        if (name == "bubble_shooter_hd2d") migrateBubbleShooterProgress(context, user, target)
        if (name == "block_puzzle_hd2d") migrateBlockPuzzleProgress(context, user, target)
        // Legacy activities still read these files with SharedPreferences.getInt(). Normalize
        // every persistent numeric counter before those reads so older cloud/JSON snapshots
        // cannot terminate a native game with ClassCastException.
        if (name == "block_puzzle") normalizeInts(target, listOf("level", "best", "score", "lines", "total_lines", "level_lines", "combo", "best_combo", "reshuffles"))
        if (name == "bubble_shooter") normalizeInts(target, listOf("level", "best_level", "best", "score", "streak", "pressure", "misses", "current", "next"))
        if (name == "knife_hit") normalizeInts(target, listOf("level", "best", "score"))
        return target
    }
    private fun intValue(prefs: SharedPreferences, key: String, fallback: Int): Int {
        val value = prefs.all[key] ?: return fallback
        return when (value) {
            is Int -> value
            is Long -> value.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
            is Float -> value.toInt()
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: fallback
            else -> fallback
        }
    }
    private fun normalizeInts(target: SharedPreferences, keys: List<String>) {
        val edit = target.edit(); var changed = false
        keys.forEach { key ->
            val raw = target.all[key] ?: return@forEach
            if (raw !is Int) {
                val parsed = when (raw) {
                    is Long -> raw.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
                    is Float -> raw.toInt()
                    is Number -> raw.toInt()
                    is String -> raw.toIntOrNull()
                    else -> null
                }
                if (parsed != null) { edit.putInt(key, parsed); changed = true }
                else { edit.remove(key); changed = true }
            }
        }
        if (changed) edit.apply()
    }
    private fun migrateBubbleShooterProgress(context: Context, user: String?, target: SharedPreferences) {
        normalizeInts(target, listOf("best", "level", "best_level", "score", "streak", "pressure", "misses", "current", "next"))
        if (target.all.isNotEmpty()) return
        val meta = context.getSharedPreferences("wet_progress_migration", Context.MODE_PRIVATE)
        val marker = "bubble_hd2d_v2_${user ?: "legacy"}"
        if (meta.getBoolean(marker, false)) return
        val legacy = forUser(context, "bubble_shooter", user)
        if (legacy.all.isNotEmpty()) {
            val best = intValue(legacy, "best", 0).coerceAtLeast(0)
            val level = maxOf(intValue(legacy, "level", 1), intValue(legacy, "best_level", 1)).coerceIn(1, 250)
            target.edit().putInt("best", best).putInt("level", level).apply()
        }
        meta.edit().putBoolean(marker, true).apply()
    }
    private fun migrateBlockPuzzleProgress(context: Context, user: String?, target: SharedPreferences) {
        normalizeInts(target, listOf("score", "best", "level", "lines", "total_lines", "level_lines", "combo", "best_combo", "reshuffles"))
        if (target.all.isNotEmpty()) return
        val meta = context.getSharedPreferences("wet_progress_migration", Context.MODE_PRIVATE)
        val marker = "block_hd2d_v2_${user ?: "legacy"}"
        if (meta.getBoolean(marker, false)) return
        val legacy = forUser(context, "block_puzzle", user)
        if (legacy.all.isNotEmpty()) {
            val best = intValue(legacy, "best", 0).coerceAtLeast(0)
            val level = intValue(legacy, "level", 1).coerceIn(1, 250)
            target.edit().putInt("best", best).putInt("level", level).apply()
        }
        meta.edit().putBoolean(marker, true).apply()
    }
    fun forUser(context: Context, name: String, user: String?): SharedPreferences =
        context.getSharedPreferences(if (user == null) name else "${name}_user_$user", Context.MODE_PRIVATE)
    fun migrateLegacy(context: Context, user: String) {
        val meta = context.getSharedPreferences("wet_progress_migration", Context.MODE_PRIVATE)
        if (meta.getString("legacy_owner", null)?.let { it != user } == true || meta.getBoolean("migrated_$user", false)) return
        (gameFiles.values.flatten() + extras).forEach { name ->
            val target = forUser(context, name, user)
            if (target.all.isEmpty()) restore(target, snapshot(context.getSharedPreferences(name, Context.MODE_PRIVATE)))
        }
        meta.edit().putString("legacy_owner", user).putBoolean("migrated_$user", true).apply()
    }
    fun snapshot(prefs: SharedPreferences): JSONObject = JSONObject().apply {
        prefs.all.toSortedMap().forEach { (key, value) ->
            if (key == "cloud_updated_at") return@forEach
            when (value) {
                is Long -> put(key, JSONObject().put("__wet_type", "long").put("value", value))
                is Float -> put(key, JSONObject().put("__wet_type", "float").put("value", value.toDouble()))
                else -> put(key, value)
            }
        }
    }
    fun restore(prefs: SharedPreferences, data: JSONObject) {
        val edit = prefs.edit().clear()
        data.keys().forEach { key ->
            when (val value = data.get(key)) {
                is Boolean -> edit.putBoolean(key, value)
                is String -> edit.putString(key, value)
                is JSONObject -> when (value.optString("__wet_type")) {
                    "long" -> edit.putLong(key, value.optLong("value"))
                    "float" -> edit.putFloat(key, value.optDouble("value").toFloat())
                }
                is Number -> if (key.endsWith("_at")) edit.putLong(key, value.toLong()) else edit.putInt(key, value.toInt())
            }
        }
        edit.apply()
    }
    fun state(context: Context, user: String, game: String): JSONObject = JSONObject().apply {
        gameFiles.getValue(game).forEach { name -> put(name, snapshot(forUser(context, name, user))) }
    }
    fun apply(context: Context, user: String, game: String, state: JSONObject) {
        gameFiles.getValue(game).forEach { name -> restore(forUser(context, name, user), state.optJSONObject(name) ?: JSONObject()) }
    }
}
