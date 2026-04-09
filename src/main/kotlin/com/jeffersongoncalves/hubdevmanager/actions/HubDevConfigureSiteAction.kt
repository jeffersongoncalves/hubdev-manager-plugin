package com.jeffersongoncalves.hubdevmanager.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager
import com.jeffersongoncalves.hubdevmanager.icons.HubDevIcons
import com.jeffersongoncalves.hubdevmanager.service.HubDevDetectorService

class HubDevConfigureSiteAction : AnAction(
    "Configure Site",
    "Configure HubDev site settings",
    HubDevIcons.HUBDEV
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("HubDev Manager")
        toolWindow?.show()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null &&
                HubDevDetectorService.getInstance().isHubDevInstalled()
    }
}
