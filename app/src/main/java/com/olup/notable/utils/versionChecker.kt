package com.olup.notable

import android.content.Context
import android.content.pm.PackageManager
import java.net.URL
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@kotlinx.serialization.Serializable
data class ghVersion(val name: String, val prerelease: Boolean, val html_url: String)

data class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version> {
    companion object {
        fun fromString(versionString: String): Version? {
            val versionRegex = """(\d+)\.(\d+)\.(\d+)""".toRegex()
            val matchResult = versionRegex.matchEntire(versionString)
            return matchResult?.destructured?.let { (major, minor, patch) ->
                Version(major.toInt(), minor.toInt(), patch.toInt())
            }
        }
    }

    override fun compareTo(other: Version): Int {
        if (this.major != other.major) {
            return this.major.compareTo(other.major)
        }
        if (this.minor != other.minor) {
            return this.minor.compareTo(other.minor)
        }
        return this.patch.compareTo(other.patch)
    }
}


fun getLatestReleaseVersion(repoOwner: String, repoName: String): String? {
    val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases"
    val json = URL(apiUrl).readText()
    val versions = Json{ ignoreUnknownKeys = true }.decodeFromString<List<ghVersion>>(json)

    versions.forEach {
        if (!it.prerelease) {
            // Check if the tag name starts with "v" and remove it if necessary
            return if (it.name.startsWith("v")) {
                it.name.substring(1)
            } else {
                it.name
            }
        }
    }

    return null
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
var isLatestVersion: Boolean? = null

fun isLatestVersion(context: Context, force: Boolean = false): Boolean {
    if (!force && isLatestVersion != null) return isLatestVersion!!

    try {
        val version = getCurrentVersionName(context)
        val latestVersion = getLatestReleaseVersion("olup", "notable")
        println("Version is ${version} and latest on repo is ${latestVersion}")

        // If either version is null, we can't compare them
        if (latestVersion == null || version == null){
            throw Exception("One of the version is null - comparison is impossible")
        }

        val versionVersion = Version.fromString(version!!)
        val latestVersionVersion = Version.fromString(latestVersion!!)

        // If either version does not fit simple semantic version don't compare
        if (latestVersionVersion == null || versionVersion == null) {
            throw Exception("One of the version doesn't match simple semantic - comparison is impossible")

        }

        isLatestVersion = versionVersion.compareTo(latestVersionVersion) != -1

        return isLatestVersion!!
    } catch (e: Exception) {
        println("Failed to fetch latest release version: ${e.message}")
        return true
    }
}

val isNext = BuildConfig.IS_NEXT