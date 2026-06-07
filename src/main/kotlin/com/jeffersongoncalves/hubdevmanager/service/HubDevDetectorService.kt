package com.jeffersongoncalves.hubdevmanager.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.jeffersongoncalves.hubdevmanager.model.HubDevSite
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Locates the HubDev installation and reads its central configuration.
 *
 * HubDev ships as `hubdev` (new brand name). On Windows the executable still
 * carries the old name `devhub.exe`, so detection must accept both. The runtime
 * config home is ~/.devhub (config/sites.yml, php/<version>/, ...).
 */
@Service(Service.Level.APP)
class HubDevDetectorService {

    private val log = Logger.getInstance(HubDevDetectorService::class.java)

    fun isHubDevInstalled(): Boolean = getHubDevExecutable() != null

    fun getHubDevExecutable(): String? {
        val os = System.getProperty("os.name", "").lowercase()
        return when {
            os.contains("win") -> findWindowsHubDev()
            os.contains("mac") || os.contains("darwin") -> findMacHubDev()
            else -> findLinuxHubDev()
        }
    }

    /** ~/.devhub — HubDev's runtime/config home directory. */
    fun getConfigHome(): Path? {
        val home = System.getProperty("user.home") ?: return null
        val dir = Paths.get(home, ".devhub")
        return if (Files.isDirectory(dir)) dir else null
    }

    private fun getSitesFile(): Path? {
        val home = getConfigHome() ?: return null
        val file = home.resolve("config").resolve("sites.yml")
        return if (Files.exists(file)) file else null
    }

    /** Absolute path to ~/.devhub/config/sites.yml regardless of existence. */
    fun getSitesFilePath(): Path? {
        val home = System.getProperty("user.home") ?: return null
        return Paths.get(home, ".devhub", "config", "sites.yml")
    }

    fun getAllSites(): List<HubDevSite> {
        val file = getSitesFile() ?: return emptyList()
        return try {
            HubDevSite.parseAll(Files.readString(file))
        } catch (e: Exception) {
            log.warn("Failed to read sites.yml", e)
            emptyList()
        }
    }

    /** Finds the registered site whose path matches [projectPath], if any. */
    fun findSiteByPath(projectPath: String?): HubDevSite? {
        if (projectPath == null) return null
        val target = HubDevSite.normalizePath(projectPath)
        return getAllSites().firstOrNull { HubDevSite.normalizePath(it.path) == target }
    }

    /**
     * Installed PHP versions, read from ~/.devhub/php/<version>/ directories.
     * Falls back to a sensible default list when the directory is unreadable.
     */
    fun getInstalledPhpVersions(): List<String> {
        val home = getConfigHome() ?: return defaultPhpVersions()
        val phpDir = home.resolve("php")
        if (!Files.isDirectory(phpDir)) return defaultPhpVersions()

        return try {
            Files.list(phpDir).use { stream ->
                stream.map { it.fileName.toString() }
                    .filter { it.matches(Regex("\\d+\\.\\d+")) }
                    .sorted(Comparator.reverseOrder())
                    .toList()
            }.ifEmpty { defaultPhpVersions() }
        } catch (e: Exception) {
            log.warn("Failed to list installed PHP versions", e)
            defaultPhpVersions()
        }
    }

    private fun findWindowsHubDev(): String? {
        // Installed app — old executable name (devhub.exe) under Program Files.
        val programFiles = System.getenv("ProgramFiles") ?: "C:\\Program Files"
        val installed = Paths.get(programFiles, "HubDev", "HubDev", "devhub.exe")
        if (Files.exists(installed)) return installed.toString()

        // Either brand name on PATH, with Windows executable extensions.
        return findInPath("hubdev") ?: findInPath("devhub")
    }

    private fun findMacHubDev(): String? {
        val home = System.getProperty("user.home")
        val candidates = listOfNotNull(
            "/opt/homebrew/bin/hubdev",
            "/usr/local/bin/hubdev",
            "/opt/homebrew/bin/devhub",
            "/usr/local/bin/devhub",
            home?.let { "$it/.devhub/bin/hubdev" },
            home?.let { "$it/.devhub/bin/devhub" },
        )
        return candidates.firstOrNull { Files.exists(Paths.get(it)) }
            ?: findInPath("hubdev")
            ?: findInPath("devhub")
    }

    private fun findLinuxHubDev(): String? {
        val home = System.getProperty("user.home")
        val candidates = listOfNotNull(
            home?.let { "$it/.devhub/bin/hubdev" },
            home?.let { "$it/.devhub/bin/devhub" },
            "/usr/local/bin/hubdev",
            "/usr/local/bin/devhub",
        )
        return candidates.firstOrNull { Files.exists(Paths.get(it)) }
            ?: findInPath("hubdev")
            ?: findInPath("devhub")
    }

    private fun findInPath(command: String): String? {
        val pathEnv = System.getenv("PATH") ?: return null
        val isWin = System.getProperty("os.name", "").lowercase().contains("win")
        val separator = if (isWin) ";" else ":"
        val extensions = if (isWin) listOf(".exe", ".bat", ".cmd", "") else listOf("")

        for (dir in pathEnv.split(separator)) {
            if (dir.isBlank()) continue
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
