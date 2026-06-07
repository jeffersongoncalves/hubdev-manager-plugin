package com.jeffersongoncalves.hubdevmanager.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager
import com.jeffersongoncalves.hubdevmanager.icons.HubDevIcons
import com.jeffersongoncalves.hubdevmanager.service.HubDevDetectorService

class HubDevManageSiteAction : AnAction(
    "Manage Site",
    "Open the HubDev Manager tool window",
    HubDevIcons.HUBDEV,
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ToolWindowManager.getInstance(project).getToolWindow("HubDev Manager")?.show()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null &&
                HubDevDetectorService.getInstance().isHubDevInstalled()
    }
}
