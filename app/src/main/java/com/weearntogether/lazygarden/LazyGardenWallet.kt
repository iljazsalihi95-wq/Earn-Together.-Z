package com.weearntogether.lazygarden

import android.content.Context

/** Persistent soft currency used only for LazyGarden restoration progression. */
class LazyGardenWallet(context: Context) {
    private val prefs = context.getSharedPreferences("lazy_garden_wallet", Context.MODE_PRIVATE)

    val coins: Int
        get() {
            if (!prefs.contains(KEY_COINS)) {
                prefs.edit().putInt(KEY_COINS, STARTING_COINS).apply()
                return STARTING_COINS
            }
            return prefs.getInt(KEY_COINS, STARTING_COINS).coerceAtLeast(0)
        }

    fun credit(amount: Int): Int {
        require(amount >= 0)
        val updated = (coins.toLong() + amount.toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        prefs.edit().putInt(KEY_COINS, updated).apply()
        return updated
    }

    fun spend(amount: Int): Boolean {
        require(amount >= 0)
        val balance = coins
        if (balance < amount) return false
        prefs.edit().putInt(KEY_COINS, balance - amount).apply()
        return true
    }

    private companion object {
        const val KEY_COINS = "coins"
        // Enough for the opening restoration only; subsequent builds are funded by earned level rewards.
        const val STARTING_COINS = 50
    }
}
