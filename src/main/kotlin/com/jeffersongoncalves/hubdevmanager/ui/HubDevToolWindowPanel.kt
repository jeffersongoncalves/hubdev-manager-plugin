package com.jeffersongoncalves.hubdevmanager.ui

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.jeffersongoncalves.hubdevmanager.icons.HubDevIcons
import com.jeffersongoncalves.hubdevmanager.service.HubDevCliService
import com.jeffersongoncalves.hubdevmanager.service.HubDevDetectorService
import com.jeffersongoncalves.hubdevmanager.service.HubDevSiteService
import javax.swing.BoxLayout
import javax.swing.JPanel

class HubDevToolWindowPanel(private val project: Project) : JPanel() {

    private val siteService = HubDevSiteService.getInstance(project)
    private val detector = HubDevDetectorService.getInstance()
    private val cli = HubDevCliService.getInstance()

    private val statusLabel = JBLabel()
    private val domainLabel = JBLabel()
    private val phpLabel = JBLabel()
    private val databaseLabel = JBLabel()
    private val modeLabel = JBLabel()

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.empty(8)
        buildUI()
        subscribeToChanges()
    }

    private fun buildUI() {
        removeAll()
        val site = siteService.site

        val content = panel {
            group("Status") {
                row { cell(statusLabel) }
                row("Domain:") {
                    cell(domainLabel)
                    button("Open in Browser") {
                        siteService.siteUrl()?.let { BrowserUtil.browse(it) }
                    }.enabled(siteService.isLinked)
                }
                row("PHP:") { cell(phpLabel) }
                row("Database:") { cell(databaseLabel) }
                row("Mode:") { cell(modeLabel) }
            }

            group("Actions") {
                row {
                    button("Link Site") { linkSite() }.enabled(!siteService.isLinked)
                    button("Unlink Site") { unlinkSite() }.enabled(siteService.isLinked)
                }
                row {
                    button("Start Site") { site?.let { startStop(it.name, start = true) } }
                        .enabled(siteService.isLinked && site?.active == false)
                    button("Stop Site") { site?.let { startStop(it.name, start = false) } }
                        .enabled(siteService.isLinked && site?.active == true)
                }
                row {
                    button("Renew SSL") { cli.secureSites(project) { refreshAsync() } }
                        .enabled(siteService.isLinked)
                }
            }

            group("HubDev") {
                row {
                    button("Open HubDev App") { openApp() }
                    button("Refresh") { siteService.reload() }
                }
            }
        }

        add(content)
        updateStatus()
        revalidate()
        repaint()
    }

    private fun updateStatus() {
        val site = siteService.site
        if (site != null) {
            statusLabel.icon = HubDevIcons.LINKED
            statusLabel.text = if (site.active) "Linked (active)" else "Linked (stopped)"
            statusLabel.foreground = JBColor(JBColor.GREEN.darker(), JBColor.GREEN)
            domainLabel.text = site.domain
            phpLabel.text = site.phpVersion
            databaseLabel.text = site.database.ifBlank { "—" }
            modeLabel.text = site.mode
        } else {
            statusLabel.icon = HubDevIcons.UNLINKED
            statusLabel.text = "Not Linked"
            statusLabel.foreground = JBColor(JBColor.ORANGE.darker(), JBColor.ORANGE)
            domainLabel.text = "—"
            phpLabel.text = "—"
            databaseLabel.text = "—"
            modeLabel.text = "—"
        }
    }

    private fun linkSite() {
        val path = project.basePath ?: return
        cli.linkSite(project, path, siteService.suggestedName()) { refreshAsync() }
    }

    private fun unlinkSite() {
        val name = siteService.site?.name ?: return
        cli.unlinkSite(project, name) { refreshAsync() }
    }

    private fun startStop(name: String, start: Boolean) {
        if (start) cli.startSite(project, name) { refreshAsync() }
        else cli.stopSite(project, name) { refreshAsync() }
    }

    private fun openApp() {
        val exe = detector.getHubDevExecutable() ?: return
        try {
            GeneralCommandLine(exe).createProcess()
        } catch (ignored: Exception) {
        }
    }

    private fun refreshAsync() {
        ApplicationManager.getApplication().invokeLater { siteService.reload() }
    }

    private fun subscribeToChanges() {
        project.messageBus.connect().subscribe(
            HubDevSiteService.SITE_CHANGED,
            object : HubDevSiteService.SiteChangeListener {
                override fun onSiteChanged() {
                    ApplicationManager.getApplication().invokeLater { buildUI() }
                }
            },
        )
    }
}
