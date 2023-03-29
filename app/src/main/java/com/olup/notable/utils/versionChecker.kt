package com.olup.notable

import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import android.content.Context
import android.content.pm.PackageManager

fun getLatestReleaseVersion(repoOwner: String, repoName: String): String? {
    val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
    val url = URL(apiUrl)
    val connection = url.openConnection()
    connection.setRequestProperty("User-Agent", "Mozilla/5.0")
    val reader = BufferedReader(InputStreamReader(connection.getInputStream()))
    val json = StringBuilder()
    var line: String?
    while (reader.readLine().also { line = it } != null) {
        json.append(line)
    }
    reader.close()

    // Parse the JSON response and extract the tag name
    val version = json.toString()
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
fun isLatestVersion(context: Context) : Boolean{
    if(isLatestVersion!=null) return isLatestVersion!!

    try {
        val version = getCurrentVersionName(context)
        val latestVersion = getLatestReleaseVersion("olup", "notable-public")
        isLatestVersion = version != latestVersion
        return isLatestVersion!!
    } catch (e: Exception) {
        println("Failed to fetch latest release version: ${e.message}")
        return true
    }
}