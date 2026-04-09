package com.jeffersongoncalves.hubdevmanager.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jeffersongoncalves.hubdevmanager.icons.HubDevIcons
import com.jeffersongoncalves.hubdevmanager.service.HubDevCliService
import com.jeffersongoncalves.hubdevmanager.service.HubDevConfigService
import com.jeffersongoncalves.hubdevmanager.service.HubDevDetectorService

class HubDevLinkSiteAction : AnAction(
    "Link Site",
    "Link this project to HubDev",
    HubDevIcons.HUBDEV
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val configService = HubDevConfigService.getInstance(project)

        if (!configService.hasConfig()) {
            val defaultConfig = configService.getDefaultConfig()
            configService.saveConfig(defaultConfig)
        }

        val config = configService.config ?: return
        val cliService = HubDevCliService.getInstance()

        cliService.linkSite(project, config.name) {
            val domain = config.domain
            if (domain.isNotBlank()) {
                cliService.secureSite(project, domain) {
                    configService.loadConfig()
                }
            } else {
                configService.loadConfig()
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null &&
                HubDevDetectorService.getInstance().isHubDevInstalled()
    }
}
