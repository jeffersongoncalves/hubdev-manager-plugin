package com.jeffersongoncalves.hubdevmanager.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jeffersongoncalves.hubdevmanager.icons.HubDevIcons
import com.jeffersongoncalves.hubdevmanager.service.HubDevConfigService
import com.jeffersongoncalves.hubdevmanager.service.HubDevDetectorService

class HubDevOpenInBrowserAction : AnAction(
    "Open in Browser",
    "Open site in default browser",
    HubDevIcons.HUBDEV
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val url = HubDevConfigService.getInstance(project).getSiteUrl() ?: return
        BrowserUtil.browse(url)
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val isAvailable = project != null &&
                HubDevDetectorService.getInstance().isHubDevInstalled() &&
                HubDevConfigService.getInstance(project).isLinked

        e.presentation.isEnabledAndVisible = isAvailable
    }
}
