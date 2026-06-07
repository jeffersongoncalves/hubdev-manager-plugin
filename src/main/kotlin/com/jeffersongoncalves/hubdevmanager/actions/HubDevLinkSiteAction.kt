package com.jeffersongoncalves.hubdevmanager.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jeffersongoncalves.hubdevmanager.icons.HubDevIcons
import com.jeffersongoncalves.hubdevmanager.service.HubDevCliService
import com.jeffersongoncalves.hubdevmanager.service.HubDevDetectorService
import com.jeffersongoncalves.hubdevmanager.service.HubDevSiteService

class HubDevLinkSiteAction : AnAction(
    "Link Site",
    "Link this project to HubDev",
    HubDevIcons.HUBDEV,
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val path = project.basePath ?: return
        val siteService = HubDevSiteService.getInstance(project)

        HubDevCliService.getInstance().linkSite(project, path, siteService.suggestedName()) {
            siteService.reload()
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val installed = HubDevDetectorService.getInstance().isHubDevInstalled()
        val linked = project != null && HubDevSiteService.getInstance(project).isLinked
        e.presentation.isEnabledAndVisible = project != null && installed && !linked
    }
}
