package com.dual.island;

import android.content.Context;
import android.content.SharedPreferences;

final class IslandConfig {
    static final String[] MODES = {"Media", "Notifications", "Shortcuts", "Timer"};
    static final int[] COLORS = {0xFF45F0C8, 0xFF9279FF, 0xFFFFAB72, 0xFF79D7FC};
    static final String[] COLOR_NAMES = {"Mint", "Violet", "Peach", "Sky"};

    static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences("dual_island_options", Context.MODE_PRIVATE);
    }

    static String k(int island, String key) { return "island_" + island + "_" + key; }
    static boolean enabled(Context ctx, int i) { return prefs(ctx).getBoolean(k(i,"enabled"), true); }
    static int mode(Context ctx, int i) { return bounded(prefs(ctx).getInt(k(i,"mode"), i == 0 ? 0 : 2), 0, 3); }
    static int color(Context ctx, int i) { return bounded(prefs(ctx).getInt(k(i,"color"), i == 0 ? 0 : 1), 0, 3); }
    static int width(Context ctx, int i) { return bounded(prefs(ctx).getInt(k(i,"width"), i == 0 ? 170 : 148), 112, 275); }
    static int opacity(Context ctx, int i) { return bounded(prefs(ctx).getInt(k(i,"opacity"), 96), 55, 100); }
    static int x(Context ctx, int i) { return prefs(ctx).getInt(k(i,"x"), -1); }
    static int y(Context ctx, int i) { return prefs(ctx).getInt(k(i,"y"), i == 0 ? 52 : 118); }
    static boolean previewNotifications(Context ctx) { return prefs(ctx).getBoolean("show_notification_text", false); }
    static void putBool(Context ctx, int i, String key, boolean value) { prefs(ctx).edit().putBoolean(k(i,key),value).apply(); }
    static void putInt(Context ctx, int i, String key, int value) { prefs(ctx).edit().putInt(k(i,key),value).apply(); }
    static int bounded(int v,int min,int max) { return Math.max(min,Math.min(max,v)); }
}
