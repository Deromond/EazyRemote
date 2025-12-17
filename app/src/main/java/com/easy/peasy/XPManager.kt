package com.easy.peasy

import android.content.Context
import android.content.SharedPreferences

object XPManager {
    private const val PREF = "xp_prefs"
    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun get(blockId: Int, ctx: Context): Int =
        prefs(ctx).getInt("xp_"+blockId, 0)

    fun add(blockId: Int, delta: Int, ctx: Context) {
        val p = prefs(ctx)
        p.edit().putInt("xp_"+blockId, get(blockId, ctx) + delta).apply()
    }

    fun reset(blockId: Int, ctx: Context) {
        prefs(ctx).edit().putInt("xp_"+blockId, 0).apply()
    }
}