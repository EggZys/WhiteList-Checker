package com.eggzys.internetmonitor

import androidx.annotation.ColorRes

enum class InternetState(
    val displayName: String,
    val emoji: String,
    val notificationText: String,
    @ColorRes val colorRes: Int,
    @ColorRes val bgColorRes: Int
) {
    FULL_ACCESS(
        "FULL ACCESS",
        "\u2714",
        "All nodes reachable. Network fully operational.",
        R.color.status_full_access,
        R.color.status_full_access_bg
    ),
    RUSSIA_ONLY(
        "RU ONLY",
        "\u26A0",
        "Global nodes blocked. Only RU network accessible.",
        R.color.status_russia_only,
        R.color.status_russia_only_bg
    ),
    WHITELIST_ONLY(
        "RKN LOCKDOWN",
        "\u2716",
        "RKN whitelist only. Network heavily restricted.",
        R.color.status_whitelist,
        R.color.status_whitelist_bg
    ),
    NO_INTERNET(
        "NO SIGNAL",
        "\u2620",
        "No internet connection detected.",
        R.color.status_no_internet,
        R.color.status_no_internet_bg
    ),
    UNKNOWN(
        "INITIALIZING",
        "\u231A",
        "Awaiting first network scan...",
        R.color.status_unknown,
        R.color.status_unknown_bg
    );
}
