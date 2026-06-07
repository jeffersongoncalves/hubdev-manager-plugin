package com.jeffersongoncalves.hubdevmanager.listeners

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.ToolWindowManager
import com.jeffersongoncalves.hubdevmanager.service.HubDevDetectorService
import com.jeffersongoncalves.hubdevmanager.service.HubDevSiteService

class HubDevProjectOpenListener : ProjectActivity {

    override suspend fun execute(project: Project) {
        if (!HubDevDetectorService.getInstance().isHubDevInstalled()) return

        val siteService = HubDevSiteService.getInstance(project)
        if (siteService.isLinked) return

        NotificationGroupManager.getInstance()
            .getNotificationGroup("HubDev Manager")
            .createNotification(
                "HubDev",
                "This project is not linked to HubDev. Would you like to link it?",
                NotificationType.INFORMATION,
            )
            .addAction(NotificationAction.createSimpleExpiring("Link Now") {
                ToolWindowManager.getInstance(project).getToolWindow("HubDev Manager")?.show()
            })
            .notify(project)
    }
}
