package com.jeffersongoncalves.hubdevmanager.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Service(Service.Level.APP)
class HubDevDetectorService {

    private val log = Logger.getInstance(HubDevDetectorService::class.java)

    fun isHubDevInstalled(): Boolean {
        return getHubDevDir() != null || getHubDevExecutable() != null
    }

    fun getHubDevDir(): Path? {
        val home = System.getProperty("user.home") ?: return null
        val devhubDir = Paths.get(home, ".devhub")
        return if (Files.isDirectory(devhubDir)) devhubDir else null
    }

    fun getHubDevExecutable(): String? {
        val os = System.getProperty("os.name", "").lowercase()

        return when {
            os.contains("win") -> findWindowsHubDev()
            os.contains("mac") || os.contains("darwin") -> findMacHubDev()
            else -> findLinuxHubDev()
        }
    }

    private fun findWindowsHubDev(): String? {
        val programFiles = Paths.get("C:\\Program Files\\devhub\\devhub.exe")
        if (Files.exists(programFiles)) return programFiles.toString()

        val programFilesNoExt = Paths.get("C:\\Program Files\\devhub\\devhub")
        if (Files.exists(programFilesNoExt)) return programFilesNoExt.toString()

        val home = System.getProperty("user.home") ?: return null
        val homeBin = Paths.get(home, ".devhub", "bin", "devhub.exe")
        if (Files.exists(homeBin)) return homeBin.toString()

        return findInPath("devhub")
    }

    private fun findMacHubDev(): String? {
        val candidates = listOf(
            "/opt/homebrew/bin/devhub",
            "/usr/local/bin/devhub",
            Paths.get(System.getProperty("user.home"), ".devhub", "bin", "devhub").toString()
        )
        return candidates.firstOrNull { Files.exists(Paths.get(it)) } ?: findInPath("devhub")
    }

    private fun findLinuxHubDev(): String? {
        val home = System.getProperty("user.home") ?: return null
        val devhubBin = Paths.get(home, ".devhub", "bin", "devhub")
        if (Files.exists(devhubBin)) return devhubBin.toString()
        return findInPath("devhub")
    }

    private fun findInPath(command: String): String? {
        val pathEnv = System.getenv("PATH") ?: return null
        val separator = if (System.getProperty("os.name", "").lowercase().contains("win")) ";" else ":"
        val extensions = if (System.getProperty("os.name", "").lowercase().contains("win"))
            listOf(".exe", ".bat", ".cmd", "") else listOf("")

        for (dir in pathEnv.split(separator)) {
            for (ext in extensions) {
                val candidate = Paths.get(dir, "$command$ext")
                if (Files.exists(candidate)) {
                    return candidate.toString()
                }
            }
        }
        return null
    }

    fun getInstalledPhpVersions(): List<String> {
        val devhubDir = getHubDevDir() ?: return defaultPhpVersions()
        val phpDir = devhubDir.resolve("php")

        if (!Files.isDirectory(phpDir)) return defaultPhpVersions()

        return try {
            Files.list(phpDir).use { stream ->
                stream
                    .filter { Files.isDirectory(it) }
                    .map { it.fileName.toString() }
                    .filter { it.matches(Regex("\\d+\\.\\d+")) }
                    .toList()
                    .sortedDescending()
            }.ifEmpty { defaultPhpVersions() }
        } catch (e: Exception) {
            log.warn("Failed to read PHP versions from devhub", e)
            defaultPhpVersions()
        }
    }

    fun getTld(): String {
        return "test"
    }

    fun getLinkedSites(): List<String> {
        return parseSitesYml().map { it["name"]?.toString() ?: "" }.filter { it.isNotBlank() }
    }

    fun isSiteLinked(siteName: String): Boolean {
        return parseSitesYml().any { it["name"]?.toString() == siteName }
    }

    fun getSiteEntry(siteName: String): Map<String, Any>? {
        return parseSitesYml().firstOrNull { it["name"]?.toString() == siteName }
    }

    private fun parseSitesYml(): List<Map<String, Any>> {
        val devhubDir = getHubDevDir() ?: return emptyList()
        val sitesYml = devhubDir.resolve("config").resolve("sites.yml")

        if (!Files.exists(sitesYml)) return emptyList()

        return try {
            val content = Files.readString(sitesYml, Charsets.UTF_8)
            val yaml = Yaml(SafeConstructor(LoaderOptions()))
            val data = yaml.load<Map<String, Any>>(content) ?: return emptyList()
            @Suppress("UNCHECKED_CAST")
            (data["sites"] as? List<Map<String, Any>>) ?: emptyList()
        } catch (e: Exception) {
            log.warn("Failed to parse sites.yml", e)
            emptyList()
        }
    }

    private fun defaultPhpVersions(): List<String> = listOf("8.4", "8.3", "8.2", "8.1")

    companion object {
        fun getInstance(): HubDevDetectorService =
            ApplicationManager.getApplication().getService(HubDevDetectorService::class.java)
    }
}
