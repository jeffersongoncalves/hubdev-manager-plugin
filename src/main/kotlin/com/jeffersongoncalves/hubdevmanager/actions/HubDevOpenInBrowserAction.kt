package com.jeffersongoncalves.hubdevmanager.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jeffersongoncalves.hubdevmanager.icons.HubDevIcons
import com.jeffersongoncalves.hubdevmanager.service.HubDevDetectorService
import com.jeffersongoncalves.hubdevmanager.service.HubDevSiteService

class HubDevOpenInBrowserAction : AnAction(
    "Open in Browser",
    "Open this HubDev site in the default browser",
    HubDevIcons.HUBDEV,
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val url = HubDevSiteService.getInstance(project).siteUrl() ?: return
        BrowserUtil.browse(url)
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null &&
                HubDevDetectorService.getInstance().isHubDevInstalled() &&
                HubDevSiteService.getInstance(project).isLinked
    }
}
