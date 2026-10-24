package com.weearntogether.play

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/** Existing unscoped saves are retained. Each online player gets an isolated offline cache. */
object ProgressPreferences {
    val gameFiles = linkedMapOf(
        "LazyGarden" to listOf("lazy_garden_progress", "lazy_garden_wallet", "lazy_garden_milestone"),
        "Block Puzzle" to listOf("block_puzzle"),
        "Bubble Shooter" to listOf("bubble_shooter")
    )
    private val extras = listOf("wet_loyalty_progress", "wet_play_history", "color_lines", "neon_stack", "merge2048")
    fun user(context: Context): String? = context.getSharedPreferences("wet_cloud_session", Context.MODE_PRIVATE).getString("user_id", null)
    fun open(context: Context, name: String): SharedPreferences = forUser(context, name, user(context))
    fun forUser(context: Context, name: String, user: String?): SharedPreferences =
        context.getSharedPreferences(if (user == null) name else "${name}_user_$user", Context.MODE_PRIVATE)
    fun migrateLegacy(context: Context, user: String) {
        val meta = context.getSharedPreferences("wet_progress_migration", Context.MODE_PRIVATE)
        if (meta.getString("legacy_owner", null)?.let { it != user } == true || meta.getBoolean("migrated_$user", false)) return
        (gameFiles.values.flatten() + extras).forEach { name ->
            val target = forUser(context, name, user)
            if (target.all.isEmpty()) restore(target, snapshot(context.getSharedPreferences(name, Context.MODE_PRIVATE)))
        }
        // Originals remain untouched as a recovery copy. No passwords are migrated.
        check(meta.edit().putString("legacy_owner", user).putBoolean("migrated_$user", true).commit())
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
        check(edit.commit())
    }
    fun state(context: Context, user: String, game: String): JSONObject = JSONObject().apply {
        gameFiles.getValue(game).forEach { name -> put(name, snapshot(forUser(context, name, user))) }
    }
    fun apply(context: Context, user: String, game: String, state: JSONObject) {
        gameFiles.getValue(game).forEach { name -> restore(forUser(context, name, user), state.optJSONObject(name) ?: JSONObject()) }
    }
}
