package com.gimranov.zandy.app

internal object Util {
    private val DOI_PREFIX = "https://doi.org/"

    fun doiToUri(doi: String): String {
        return if (isDoi(doi)) {
            DOI_PREFIX + doi.replace("^doi:".toRegex(), "")
        } else doi
    }

    private fun isDoi(doi: String): Boolean {
        return doi.startsWith("doi:") || doi.startsWith("10.")
    }
}
