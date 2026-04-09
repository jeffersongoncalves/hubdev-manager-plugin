package com.jeffersongoncalves.hubdevmanager.service

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.jeffersongoncalves.hubdevmanager.model.DatabaseConfig
import com.jeffersongoncalves.hubdevmanager.model.HubDevConfig
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.representer.Representer
import java.nio.file.Files
import java.nio.file.Paths

@Service(Service.Level.APP)
class HubDevCliService {

    private val log = Logger.getInstance(HubDevCliService::class.java)

    fun linkSite(project: Project, config: HubDevConfig, onComplete: (() -> Unit)? = null) {
        val projectPath = project.basePath ?: return

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Linking site '${config.name}'...", false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val siteEntry = linkedMapOf<String, Any>(
                        "name" to config.name,
                        "domain" to config.domain,
                        "path" to projectPath.replace("/", "\\"),
                        "doc_root" to "${projectPath.replace("/", "\\")}\\public",
                        "driver" to "laravel",
                        "php_version" to config.php,
                        "database" to config.database.name,
                        "active" to true,
                        "mode" to "traditional",
                    )

                    addSiteToYml(config.name, siteEntry)

                    ApplicationManager.getApplication().invokeLater {
                        showNotification(project, "Site '${config.name}' linked successfully", NotificationType.INFORMATION)
                        onComplete?.invoke()
                    }
                } catch (e: Exception) {
                    log.error("Failed to link site '${config.name}'", e)
                    ApplicationManager.getApplication().invokeLater {
                        showNotification(project, "Failed to link site: ${e.message}", NotificationType.ERROR)
                        onComplete?.invoke()
                    }
                }
            }
        })
    }

    fun unlinkSite(project: Project, siteName: String, onComplete: (() -> Unit)? = null) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Unlinking site '$siteName'...", false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    removeSiteFromYml(siteName)

                    ApplicationManager.getApplication().invokeLater {
                        showNotification(project, "Site '$siteName' unlinked successfully", NotificationType.INFORMATION)
                        onComplete?.invoke()
                    }
                } catch (e: Exception) {
                    log.error("Failed to unlink site '$siteName'", e)
                    ApplicationManager.getApplication().invokeLater {
                        showNotification(project, "Failed to unlink site: ${e.message}", NotificationType.ERROR)
                        onComplete?.invoke()
                    }
                }
            }
        })
    }

    fun openSite(@Suppress("UNUSED_PARAMETER") project: Project, domain: String) {
        BrowserUtil.browse("http://$domain")
    }

    fun runScript(project: Project, scriptName: String, scriptCommand: String, onComplete: (() -> Unit)? = null) {
        val title = "Running '$scriptName'..."
        val os = System.getProperty("os.name", "").lowercase()

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, title, false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val commandLine = if (os.contains("win")) {
                        GeneralCommandLine("cmd", "/c", scriptCommand)
                    } else {
                        GeneralCommandLine("bash", "-c", scriptCommand)
                    }
                        .withWorkDirectory(project.basePath)
                        .withCharset(Charsets.UTF_8)

                    val handler = CapturingProcessHandler(commandLine)
                    val result = handler.runProcess(30_000)

                    ApplicationManager.getApplication().invokeLater {
                        if (result.exitCode == 0) {
                            val output = result.stdout.trim().ifEmpty { "Script '$scriptName' completed successfully" }
                            showNotification(project, output, NotificationType.INFORMATION)
                        } else {
                            val error = result.stderr.trim().ifEmpty { result.stdout.trim() }
                            showNotification(project, "Script '$scriptName' failed: $error", NotificationType.ERROR)
                        }
                        onComplete?.invoke()
                    }
                } catch (e: Exception) {
                    log.error("Failed to execute script '$scriptName': $scriptCommand", e)
                    ApplicationManager.getApplication().invokeLater {
                        showNotification(project, "Script '$scriptName' failed: ${e.message}", NotificationType.ERROR)
                        onComplete?.invoke()
                    }
                }
            }
        })
    }

    fun createDatabase(project: Project, config: DatabaseConfig, onComplete: (() -> Unit)? = null) {
        showNotification(project, "Database creation requires HubDev CLI (not yet available). Configure your database manually.", NotificationType.WARNING)
        onComplete?.invoke()
    }

    private fun getSitesYmlPath(): java.nio.file.Path? {
        val home = System.getProperty("user.home") ?: return null
        return Paths.get(home, ".devhub", "config", "sites.yml")
    }

    private fun readSitesYml(): MutableList<MutableMap<String, Any>> {
        val path = getSitesYmlPath() ?: return mutableListOf()
        if (!Files.exists(path)) return mutableListOf()

        return try {
            val content = Files.readString(path, Charsets.UTF_8)
            val yaml = Yaml(SafeConstructor(LoaderOptions()))
            val data = yaml.load<Map<String, Any>>(content) ?: return mutableListOf()
            @Suppress("UNCHECKED_CAST")
            val sites = data["sites"] as? List<Map<String, Any>> ?: return mutableListOf()
            sites.map { it.toMutableMap() }.toMutableList()
        } catch (e: Exception) {
            log.warn("Failed to read sites.yml", e)
            mutableListOf()
        }
    }

    private fun writeSitesYml(sites: List<Map<String, Any>>) {
        val path = getSitesYmlPath() ?: return
        Files.createDirectories(path.parent)

        val options = DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
            indent = 4
            indicatorIndent = 0
        }
        val yaml = Yaml(SafeConstructor(LoaderOptions()), Representer(options), options)
        val data = linkedMapOf<String, Any>("sites" to sites)
        val content = yaml.dump(data)
        Files.writeString(path, content, Charsets.UTF_8)
    }

    private fun addSiteToYml(siteName: String, siteEntry: Map<String, Any>) {
        val sites = readSitesYml()
        sites.removeIf { it["name"]?.toString() == siteName }
        sites.add(siteEntry.toMutableMap())
        writeSitesYml(sites)
    }

    private fun removeSiteFromYml(siteName: String) {
        val sites = readSitesYml()
        sites.removeIf { it["name"]?.toString() == siteName }
        writeSitesYml(sites)
    }

    private fun showNotification(project: Project, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("HubDev Manager")
            .createNotification("HubDev Manager", content, type)
            .notify(project)
    }

    companion object {
        fun getInstance(): HubDevCliService =
            ApplicationManager.getApplication().getService(HubDevCliService::class.java)
    }
}
