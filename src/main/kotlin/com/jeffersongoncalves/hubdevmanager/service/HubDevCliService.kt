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

/**
 * Runs HubDev CLI commands (`hubdev`/`devhub`) and reports the result as IDE
 * notifications. Commands run in a background task with a timeout.
 */
@Service(Service.Level.APP)
class HubDevCliService {

    private val log = Logger.getInstance(HubDevCliService::class.java)

    /** site:link <path> [domain] — registers a project directory as a site. */
    fun linkSite(project: Project, path: String, domain: String?, onComplete: (() -> Unit)? = null) {
        val args = buildList {
            add("site:link")
            add(path)
            if (!domain.isNullOrBlank()) add(domain)
        }
        run(project, args, "Linking site...", onComplete)
    }

    /** site:unlink <name> */
    fun unlinkSite(project: Project, name: String, onComplete: (() -> Unit)? = null) =
        run(project, listOf("site:unlink", name), "Unlinking '$name'...", onComplete)

    /** site:start <name> — activate (re-add Caddy route). */
    fun startSite(project: Project, name: String, onComplete: (() -> Unit)? = null) =
        run(project, listOf("site:start", name), "Starting '$name'...", onComplete)

    /** site:stop <name> — deactivate (remove Caddy route). */
    fun stopSite(project: Project, name: String, onComplete: (() -> Unit)? = null) =
        run(project, listOf("site:stop", name), "Stopping '$name'...", onComplete)

    /** site:secure — renew SSL certificates for active sites. */
    fun secureSites(project: Project, onComplete: (() -> Unit)? = null) =
        run(project, listOf("site:secure"), "Renewing SSL certificates...", onComplete)

    /** site:open <domain> — open the site in the default browser. */
    fun openSite(project: Project, domain: String) =
        run(project, listOf("site:open", domain), "Opening '$domain'...")

    private fun run(
        project: Project,
        args: List<String>,
        title: String,
        onComplete: (() -> Unit)? = null,
    ) {
        val exe = HubDevDetectorService.getInstance().getHubDevExecutable()
        if (exe == null) {
            notify(project, "HubDev executable not found", NotificationType.ERROR)
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, title, false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val commandLine = GeneralCommandLine(exe)
                        .withParameters(args)
                        .withWorkDirectory(project.basePath)
                        .withCharset(Charsets.UTF_8)

                    val result = CapturingProcessHandler(commandLine).runProcess(60_000)

                    ApplicationManager.getApplication().invokeLater {
                        if (result.exitCode == 0) {
                            val out = result.stdout.trim().ifEmpty { "Command completed successfully" }
                            notify(project, out, NotificationType.INFORMATION)
                        } else {
                            val err = result.stderr.trim().ifEmpty { result.stdout.trim() }
                                .ifEmpty { "Command failed (exit ${result.exitCode})" }
                            notify(project, "Error: $err", NotificationType.ERROR)
                        }
                        onComplete?.invoke()
                    }
                } catch (e: Exception) {
                    log.error("Failed to run hubdev command: ${args.joinToString(" ")}", e)
                    ApplicationManager.getApplication().invokeLater {
                        notify(project, "Failed: ${e.message}", NotificationType.ERROR)
                        onComplete?.invoke()
                    }
                }
            }
        })
    }

    private fun notify(project: Project, content: String, type: NotificationType) {
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
