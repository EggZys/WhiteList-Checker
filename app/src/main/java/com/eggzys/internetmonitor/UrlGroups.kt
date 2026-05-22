package com.eggzys.internetmonitor

data class UrlGroups(
    val globalUrls: List<String>,
    val russiaUrls: List<String>,
    val whitelistUrls: List<String>
) {
    companion object {
        fun defaults() = UrlGroups(
            globalUrls = listOf(
                "https://www.google.com",
                "https://github.com",
                "https://cloudflare.com"
            ),
            russiaUrls = listOf(
                "https://kp40.ru",
                "https://rbc.ru",
                "https://1tv.ru"
            ),
            whitelistUrls = listOf(
                "https://dzen.ru",
                "https://gosuslugi.ru",
                "https://vk.com"
            )
        )
    }
}
