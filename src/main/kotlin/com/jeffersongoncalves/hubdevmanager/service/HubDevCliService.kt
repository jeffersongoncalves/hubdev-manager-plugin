package com.jeffersongoncalves.hubdevmanager.service

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

@Service(Service.Level.APP)
class HubDevCliService {

    private val log = Logger.getInstance(HubDevCliService::class.java)

    fun linkSite(project: Project, siteName: String, onComplete: (() -> Unit)? = null) {
        runHubDevCommand(project, listOf("link", siteName), "Linking site '$siteName'...") {
            onComplete?.invoke()
        }
    }

    fun unlinkSite(project: Project, siteName: String, onComplete: (() -> Unit)? = null) {
        runHubDevCommand(project, listOf("unlink", siteName), "Unlinking site '$siteName'...") {
            onComplete?.invoke()
        }
    }

    fun secureSite(project: Project, domain: String, onComplete: (() -> Unit)? = null) {
        runHubDevCommand(project, listOf("secure", domain), "Securing '$domain'...") {
            onComplete?.invoke()
        }
    }

    fun unsecureSite(project: Project, domain: String, onComplete: (() -> Unit)? = null) {
        runHubDevCommand(project, listOf("unsecure", domain), "Removing SSL from '$domain'...") {
            onComplete?.invoke()
        }
    }

    fun openSite(project: Project, domain: String) {
        runHubDevCommand(project, listOf("open", domain), "Opening '$domain'...")
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

    fun createDatabase(project: Project, config: com.jeffersongoncalves.hubdevmanager.model.DatabaseConfig, onComplete: (() -> Unit)? = null) {
        val args = mutableListOf("db:create",
            "--driver=${config.driver}",
            "--name=${config.name}",
            "--user=${config.user}",
        )
        if (config.password.isNotEmpty()) {
            args.add("--password=${config.password}")
        }
        runHubDevCommand(project, args, "Creating database '${config.name}'...") {
            onComplete?.invoke()
        }
    }

    private fun runHubDevCommand(
        project: Project,
        args: List<String>,
        title: String,
        onComplete: (() -> Unit)? = null
    ) {
        val hubdevExe = HubDevDetectorService.getInstance().getHubDevExecutable()
        if (hubdevExe == null) {
            showNotification(project, "HubDev executable not found", NotificationType.ERROR)
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, title, false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val commandLine = GeneralCommandLine(hubdevExe)
                        .withParameters(args)
                        .withWorkDirectory(project.basePath)
                        .withCharset(Charsets.UTF_8)

                    val handler = CapturingProcessHandler(commandLine)
                    val result = handler.runProcess(30_000)

                    ApplicationManager.getApplication().invokeLater {
                        if (result.exitCode == 0) {
                            val output = result.stdout.trim().ifEmpty { "Command completed successfully" }
                            showNotification(project, output, NotificationType.INFORMATION)
                        } else {
                            val error = result.stderr.trim().ifEmpty { result.stdout.trim() }
                            showNotification(project, "Error: $error", NotificationType.ERROR)
                        }
                        onComplete?.invoke()
                    }
                } catch (e: Exception) {
                    log.error("Failed to execute hubdev command: ${args.joinToString(" ")}", e)
                    ApplicationManager.getApplication().invokeLater {
                        showNotification(project, "Failed: ${e.message}", NotificationType.ERROR)
                        onComplete?.invoke()
                    }
                }
            }
        })
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
