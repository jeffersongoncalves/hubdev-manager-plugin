package com.jeffersongoncalves.hubdevmanager.service

import com.google.gson.JsonParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Service(Service.Level.APP)
class HubDevDetectorService {

    private val log = Logger.getInstance(HubDevDetectorService::class.java)

    fun isHubDevInstalled(): Boolean {
        return getHubDevExecutable() != null
    }

    fun getHubDevExecutable(): String? {
        val os = System.getProperty("os.name", "").lowercase()

        return when {
            os.contains("win") -> findWindowsHubDev()
            os.contains("mac") || os.contains("darwin") -> findMacHubDev()
            else -> findLinuxHubDev()
        }
    }

    fun getHubDevConfigDir(): Path? {
        val home = System.getProperty("user.home") ?: return null
        val configDir = Paths.get(home, ".config", "hubdev")
        return if (Files.isDirectory(configDir)) configDir else null
    }

    fun getInstalledPhpVersions(): List<String> {
        val configDir = getHubDevConfigDir() ?: return defaultPhpVersions()
        val phpJson = configDir.resolve("config").resolve("php.json")

        if (!Files.exists(phpJson)) return defaultPhpVersions()

        return try {
            val content = Files.readString(phpJson)
            val json = JsonParser.parseString(content).asJsonObject
            json.keySet()
                .filter { it.startsWith("installed_") && !it.startsWith("installed_internal_") }
                .map { it.removePrefix("installed_") }
                .filter { it.matches(Regex("\\d+\\.\\d+")) }
                .sortedDescending()
        } catch (e: Exception) {
            log.warn("Failed to read PHP versions from hubdev config", e)
            defaultPhpVersions()
        }
    }

    fun getTld(): String {
        val configDir = getHubDevConfigDir() ?: return "test"
        val settingsJson = configDir.resolve("config").resolve("settings.json")

        if (!Files.exists(settingsJson)) return "test"

        return try {
            val content = Files.readString(settingsJson)
            val json = JsonParser.parseString(content).asJsonObject
            json.get("tld")?.asString ?: "test"
        } catch (e: Exception) {
            log.warn("Failed to read TLD from hubdev config", e)
            "test"
        }
    }

    fun getLinkedSites(): List<String> {
        val configDir = getHubDevConfigDir() ?: return emptyList()
        val sitesDir = configDir.resolve("sites")

        if (!Files.isDirectory(sitesDir)) return emptyList()

        return try {
            Files.list(sitesDir).use { stream ->
                stream.map { it.fileName.toString() }.toList()
            }
        } catch (e: Exception) {
            log.warn("Failed to list linked sites", e)
            emptyList()
        }
    }

    fun isSiteLinked(siteName: String): Boolean {
        val configDir = getHubDevConfigDir() ?: return false
        val sitePath = configDir.resolve("sites").resolve(siteName)
        return Files.exists(sitePath)
    }

    private fun findWindowsHubDev(): String? {
        val home = System.getProperty("user.home") ?: return null
        val hubdevBat = Paths.get(home, ".config", "hubdev", "bin", "hubdev.bat")
        if (Files.exists(hubdevBat)) return hubdevBat.toString()

        val hubdevExe = Paths.get(home, ".config", "hubdev", "bin", "hubdev.exe")
        if (Files.exists(hubdevExe)) return hubdevExe.toString()

        return findInPath("hubdev")
    }

    private fun findMacHubDev(): String? {
        val candidates = listOf(
            "/opt/homebrew/bin/hubdev",
            "/usr/local/bin/hubdev",
            Paths.get(System.getProperty("user.home"), ".config", "hubdev", "bin", "hubdev").toString()
        )
        return candidates.firstOrNull { Files.exists(Paths.get(it)) } ?: findInPath("hubdev")
    }

    private fun findLinuxHubDev(): String? {
        val home = System.getProperty("user.home") ?: return null
        val hubdevBin = Paths.get(home, ".config", "hubdev", "bin", "hubdev")
        if (Files.exists(hubdevBin)) return hubdevBin.toString()
        return findInPath("hubdev")
    }

    private fun findInPath(command: String): String? {
        val pathEnv = System.getenv("PATH") ?: return null
        val separator = if (System.getProperty("os.name", "").lowercase().contains("win")) ";" else ":"
        val extensions = if (System.getProperty("os.name", "").lowercase().contains("win"))
            listOf(".bat", ".exe", ".cmd", "") else listOf("")

        for (dir in pathEnv.split(separator)) {
            for (ext in extensions) {
                val candidate = Paths.get(dir, "$command$ext")
                if (Files.exists(candidate) && Files.isExecutable(candidate)) {
                    return candidate.toString()
                }
            }
        }
        return null
    }

    private fun defaultPhpVersions(): List<String> = listOf("8.4", "8.3", "8.2", "8.1")

    companion object {
        fun getInstance(): HubDevDetectorService =
            ApplicationManager.getApplication().getService(HubDevDetectorService::class.java)
    }
}
