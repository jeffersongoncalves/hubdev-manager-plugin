package com.jeffersongoncalves.hubdevmanager.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.jeffersongoncalves.hubdevmanager.icons.HubDevIcons
import com.jeffersongoncalves.hubdevmanager.service.HubDevCliService
import com.jeffersongoncalves.hubdevmanager.service.HubDevConfigService
import com.jeffersongoncalves.hubdevmanager.service.HubDevDetectorService

class HubDevRunScriptAction : AnAction(
    "Run Script...",
    "Run a devhub.yml script",
    HubDevIcons.HUBDEV
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val configService = HubDevConfigService.getInstance(project)
        val config = configService.config ?: return
        val scripts = config.scripts
        if (scripts.isEmpty()) return

        val scriptNames = scripts.keys.toList()

        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(scriptNames)
            .setTitle("Run Script")
            .setItemChosenCallback { scriptName ->
                val command = scripts[scriptName] ?: return@setItemChosenCallback
                HubDevCliService.getInstance().runScript(project, scriptName, command)
            }
            .createPopup()
            .showInBestPositionFor(e.dataContext)
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val isAvailable = project != null &&
                HubDevDetectorService.getInstance().isHubDevInstalled() &&
                HubDevConfigService.getInstance(project).config?.scripts?.isNotEmpty() == true

        e.presentation.isEnabledAndVisible = isAvailable
    }
}
