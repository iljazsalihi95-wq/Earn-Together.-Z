package com.weearntogether.lazygarden

import android.content.Context

/** Persistent in-game currency used only inside LazyGarden. No expiry. */
class LazyGardenWallet(context: Context) {
    private val prefs = context.getSharedPreferences("lazy_garden_wallet", Context.MODE_PRIVATE)

    val coins: Int
        get() {
            if (!prefs.contains(KEY_COINS)) {
                prefs.edit().putInt(KEY_COINS, STARTING_COINS).apply()
                return STARTING_COINS
            }
            return prefs.getInt(KEY_COINS, STARTING_COINS)
        }

    fun credit(amount: Int): Int {
        require(amount >= 0)
        val updated = coins + amount
        prefs.edit().putInt(KEY_COINS, updated).apply()
        return updated
    }

    fun spend(amount: Int): Boolean {
        require(amount >= 0)
        if (coins < amount) return false
        prefs.edit().putInt(KEY_COINS, coins - amount).apply()
        return true
    }

    private companion object {
        const val KEY_COINS = "coins"
        const val STARTING_COINS = 9_999
    }
}
