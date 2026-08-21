package com.digitaldiscipline.spike.detection

import android.view.accessibility.AccessibilityNodeInfo

object BrowserDomainScanner {

    val BROWSER_PACKAGES = setOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary",
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta",
        "org.mozilla.focus",
        "com.sec.android.app.sbrowser",
        "com.sec.android.app.sbrowser.beta",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.opera.touch",
        "com.brave.browser",
        "com.microsoft.emmx",
        "com.vivo.browser",
        "com.heytap.browser",
        "com.mi.globalbrowser",
        "com.android.browser",
        "com.duckduckgo.mobile.android",
        "com.ucmobile.intl",
        "com.ecosia.android"
    )

    private val RESTRICTED_DOMAINS = listOf(
        "instagram.com",
        "youtube.com/shorts",
        "m.youtube.com",
        "tiktok.com",
        "facebook.com",
        "m.facebook.com",
        "x.com",
        "twitter.com",
        "reddit.com",
        "snapchat.com",
        "pinterest.com"
    )

    fun isBrowserPackage(packageName: String?): Boolean {
        return packageName != null && BROWSER_PACKAGES.contains(packageName)
    }

    fun scanForRestrictedDomain(rootNode: AccessibilityNodeInfo?, packageName: String): String? {
        if (rootNode == null || !isBrowserPackage(packageName)) return null

        return try {
            findMatchingDomain(rootNode)
        } catch (_: Throwable) {
            null
        }
    }

    private fun findMatchingDomain(node: AccessibilityNodeInfo): String? {
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val combined = "$text $desc"

        for (domain in RESTRICTED_DOMAINS) {
            if (combined.contains(domain)) {
                return domain
            }
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            val found = findMatchingDomain(child)
            try {
                child.recycle()
            } catch (_: Throwable) {}
            if (found != null) return found
        }

        return null
    }
}
