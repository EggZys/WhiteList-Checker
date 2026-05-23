package com.eggzys.internetmonitor

data class UrlGroups(
    val globalUrls: List<String>,
    val russiaUrls: List<String>,
    val whitelistUrls: List<String>
) {
    companion object {
        fun defaults() = UrlGroups(
            globalUrls = listOf(
                "https://ru.yummyani.me",
                "https://wikipedia.org"
            ),
            russiaUrls = listOf(
                "https://sberbank.ru",
                "https://tbank.ru"
            ),
            whitelistUrls = listOf(
                "https://nalog.gov.ru",
                "https://gosuslugi.ru"
            )
        )
    }
}
