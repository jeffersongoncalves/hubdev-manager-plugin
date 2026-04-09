package com.jeffersongoncalves.hubdevmanager.ui

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.jeffersongoncalves.hubdevmanager.icons.HubDevIcons
import com.jeffersongoncalves.hubdevmanager.model.DatabaseConfig
import com.jeffersongoncalves.hubdevmanager.model.HubDevConfig
import com.jeffersongoncalves.hubdevmanager.service.HubDevCliService
import com.jeffersongoncalves.hubdevmanager.service.HubDevConfigService
import com.jeffersongoncalves.hubdevmanager.service.HubDevDetectorService
import java.awt.BorderLayout
import javax.swing.*

class HubDevToolWindowPanel(private val project: Project) : JPanel() {

    private val configService = HubDevConfigService.getInstance(project)
    private val detectorService = HubDevDetectorService.getInstance()
    private val cliService = HubDevCliService.getInstance()

    private val siteNameField = JTextField(20)
    private val domainField = JTextField(20)
    private val phpVersionCombo = JComboBox<String>()
    private val dbDriverCombo = JComboBox<String>(arrayOf("mysql", "pgsql", "sqlite", "sqlsrv"))
    private val dbNameField = JTextField(20)
    private val dbUserField = JTextField(20)
    private val dbPasswordField = JPasswordField(20)
    private val statusLabel = JBLabel()
    private val urlLabel = JBLabel()

    private val scriptRows = mutableListOf<ScriptRow>()

    init {
        layout = BorderLayout()
        border = JBUI.Borders.empty(8)

        populatePhpVersions()
        loadCurrentConfig()
        buildUI()
        subscribeToChanges()
        setupDomainAutoUpdate()
    }

    private data class ScriptRow(
        val nameField: JTextField,
        val commandField: JTextField,
    )

    private fun buildUI() {
        removeAll()

        val mainPanel = panel {
            group("Status") {
                row {
                    cell(statusLabel)
                }
                row("URL:") {
                    cell(urlLabel)
                    button("Open in Browser") {
                        val url = configService.getSiteUrl()
                        if (url != null) {
                            BrowserUtil.browse(url)
                        }
                    }.enabled(configService.isLinked)
                }
            }

            group("Site Configuration") {
                row("Site Name:") {
                    cell(siteNameField).align(AlignX.FILL)
                }
                row("Domain:") {
                    cell(domainField).align(AlignX.FILL)
                }
                row("PHP Version:") {
                    cell(phpVersionCombo)
                }
            }

            group("Database Configuration") {
                row("Driver:") {
                    cell(dbDriverCombo)
                }
                row("Database Name:") {
                    cell(dbNameField).align(AlignX.FILL)
                }
                row("Username:") {
                    cell(dbUserField).align(AlignX.FILL)
                }
                row("Password:") {
                    cell(dbPasswordField).align(AlignX.FILL)
                }
                row {
                    button("Create Database") { createDatabase() }
                }
            }

            group("Scripts") {
                for ((index, scriptRow) in scriptRows.withIndex()) {
                    row {
                        cell(scriptRow.nameField).align(AlignX.FILL).resizableColumn()
                        cell(scriptRow.commandField).align(AlignX.FILL).resizableColumn()
                        button("\u25B6") {
                            val name = scriptRow.nameField.text.trim()
                            val command = scriptRow.commandField.text.trim()
                            if (name.isNotBlank() && command.isNotBlank()) {
                                cliService.runScript(project, name, command)
                            }
                        }
                        button("Remove") {
                            scriptRows.remove(scriptRow)
                            buildUI()
                        }
                    }
                }
                row {
                    button("Add Script") {
                        scriptRows.add(ScriptRow(JTextField(10), JTextField(20)))
                        buildUI()
                    }
                }
            }

            group("Actions") {
                row {
                    button("Save Configuration") { saveConfig() }
                    button("Link Site") { linkSite() }
                }
                row {
                    button("Unlink Site") { unlinkSite() }.enabled(configService.isLinked)
                }
            }
        }

        val scrollPane = JBScrollPane(mainPanel)
        add(scrollPane, BorderLayout.CENTER)
        updateStatus()
        revalidate()
        repaint()
    }

    private fun populatePhpVersions() {
        phpVersionCombo.removeAllItems()
        val versions = detectorService.getInstalledPhpVersions()
        for (version in versions) {
            phpVersionCombo.addItem(version)
        }
    }

    private fun loadCurrentConfig() {
        val config = configService.config
        if (config != null) {
            siteNameField.text = config.name
            domainField.text = config.domain
            phpVersionCombo.selectedItem = config.php
            dbDriverCombo.selectedItem = config.database.driver
            dbNameField.text = config.database.name
            dbUserField.text = config.database.user
            dbPasswordField.text = config.database.password
            loadScriptRows(config.scripts)
        } else {
            val defaultConfig = configService.getDefaultConfig()
            siteNameField.text = defaultConfig.name
            domainField.text = defaultConfig.domain
            phpVersionCombo.selectedItem = defaultConfig.php
            dbDriverCombo.selectedItem = defaultConfig.database.driver
            dbNameField.text = defaultConfig.database.name
            dbUserField.text = defaultConfig.database.user
            dbPasswordField.text = defaultConfig.database.password
            loadScriptRows(defaultConfig.scripts)
        }
    }

    private fun loadScriptRows(scripts: Map<String, String>) {
        scriptRows.clear()
        for ((name, command) in scripts) {
            val nameField = JTextField(10)
            nameField.text = name
            val commandField = JTextField(20)
            commandField.text = command
            scriptRows.add(ScriptRow(nameField, commandField))
        }
    }

    private fun updateStatus() {
        if (configService.isLinked) {
            statusLabel.icon = HubDevIcons.LINKED
            statusLabel.text = "Linked"
            statusLabel.foreground = JBColor(JBColor.GREEN.darker(), JBColor.GREEN)
        } else if (configService.hasConfig()) {
            statusLabel.icon = HubDevIcons.UNLINKED
            statusLabel.text = "Not Linked"
            statusLabel.foreground = JBColor(JBColor.ORANGE.darker(), JBColor.ORANGE)
        } else {
            statusLabel.icon = HubDevIcons.UNLINKED
            statusLabel.text = "Not Configured"
            statusLabel.foreground = JBColor.GRAY
        }

        val url = configService.getSiteUrl()
        urlLabel.text = url ?: "N/A"
    }

    private fun buildConfigFromUI(): HubDevConfig {
        val scripts = mutableMapOf<String, String>()
        for (row in scriptRows) {
            val name = row.nameField.text.trim()
            val command = row.commandField.text.trim()
            if (name.isNotBlank() && command.isNotBlank()) {
                scripts[name] = command
            }
        }

        return HubDevConfig(
            name = siteNameField.text.trim(),
            domain = domainField.text.trim(),
            php = phpVersionCombo.selectedItem?.toString() ?: "8.4",
            database = DatabaseConfig(
                driver = dbDriverCombo.selectedItem?.toString() ?: "mysql",
                name = dbNameField.text.trim(),
                user = dbUserField.text.trim(),
                password = String(dbPasswordField.password),
            ),
            scripts = scripts,
        )
    }

    private fun saveConfig() {
        configService.saveConfig(buildConfigFromUI())
    }

    private fun createDatabase() {
        val config = buildConfigFromUI()
        cliService.createDatabase(project, config.database)
    }

    private fun linkSite() {
        val siteName = siteNameField.text.trim()
        if (siteName.isBlank()) return

        if (!configService.hasConfig()) {
            saveConfig()
        }

        cliService.linkSite(project, siteName) {
            ApplicationManager.getApplication().invokeLater {
                val domain = domainField.text.trim()
                if (domain.isNotBlank()) {
                    cliService.secureSite(project, domain) {
                        ApplicationManager.getApplication().invokeLater {
                            configService.loadConfig()
                            refreshUI()
                        }
                    }
                } else {
                    configService.loadConfig()
                    refreshUI()
                }
            }
        }
    }

    private fun unlinkSite() {
        val siteName = siteNameField.text.trim()
        if (siteName.isBlank()) return

        cliService.unlinkSite(project, siteName) {
            ApplicationManager.getApplication().invokeLater {
                configService.loadConfig()
                refreshUI()
            }
        }
    }

    private fun refreshUI() {
        loadCurrentConfig()
        updateStatus()
        buildUI()
    }

    private fun setupDomainAutoUpdate() {
        siteNameField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = updateDomain()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = updateDomain()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = updateDomain()

            private fun updateDomain() {
                val name = siteNameField.text.trim()
                if (name.isNotBlank()) {
                    val tld = detectorService.getTld()
                    domainField.text = "$name.$tld"
                }
            }
        })
    }

    private fun subscribeToChanges() {
        project.messageBus.connect().subscribe(
            HubDevConfigService.CONFIG_CHANGED,
            object : HubDevConfigService.ConfigChangeListener {
                override fun onConfigChanged() {
                    ApplicationManager.getApplication().invokeLater {
                        loadCurrentConfig()
                        updateStatus()
                    }
                }
            }
        )
    }
}
