package com.olup.notable

import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope

fun getLatestReleaseVersion(repoOwner: String, repoName: String): String? {
    val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
    val json = URL(apiUrl).readText()

    // Parse the JSON response and extract the tag name
    val version = json
        .split("\"tag_name\":")[1]
        .split(",")[0]
        .replace("\"", "")
        .trim()

    // Check if the tag name starts with "v" and remove it if necessary
    return if (version.startsWith("v")) {
        version.substring(1)
    } else {
        version
    }
}



fun getCurrentVersionName(context: Context): String? {
    try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return packageInfo.versionName
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    return null
}

// cache
var isLatestVersion : Boolean? = null
fun isLatestVersion(context: Context, force : Boolean = false) : Boolean{
    if(!force && isLatestVersion!=null) return isLatestVersion!!

    try {
        val version = getCurrentVersionName(context)
        val latestVersion = getLatestReleaseVersion("olup", "notable")
        isLatestVersion = version != latestVersion
        return isLatestVersion!!
    } catch (e: Exception) {
        println("Failed to fetch latest release version: ${e.message}")
        return true
    }
}