package com.jeffersongoncalves.hubdevmanager.listeners

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.ToolWindowManager
import com.jeffersongoncalves.hubdevmanager.service.HubDevConfigService
import com.jeffersongoncalves.hubdevmanager.service.HubDevDetectorService

class HubDevProjectOpenListener : ProjectActivity {

    override suspend fun execute(project: Project) {
        val detector = HubDevDetectorService.getInstance()
        if (!detector.isHubDevInstalled()) return

        val configService = HubDevConfigService.getInstance(project)

        if (!configService.hasConfig()) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("HubDev Manager")
                .createNotification(
                    "HubDev",
                    "No devhub.yml found. Would you like to configure this project for HubDev?",
                    NotificationType.INFORMATION
                )
                .addAction(NotificationAction.createSimpleExpiring("Configure Now") {
                    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("HubDev Manager")
                    toolWindow?.show()
                })
                .notify(project)
        } else if (!configService.isLinked) {
            val siteName = configService.config?.name ?: return
            NotificationGroupManager.getInstance()
                .getNotificationGroup("HubDev Manager")
                .createNotification(
                    "HubDev",
                    "Site '$siteName' is configured but not linked. Would you like to link it?",
                    NotificationType.INFORMATION
                )
                .addAction(NotificationAction.createSimpleExpiring("Link Now") {
                    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("HubDev Manager")
                    toolWindow?.show()
                })
                .notify(project)
        }
    }
}
