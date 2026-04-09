package com.jeffersongoncalves.hubdevmanager.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.Consumer
import com.jeffersongoncalves.hubdevmanager.icons.HubDevIcons
import com.jeffersongoncalves.hubdevmanager.service.HubDevConfigService
import com.jeffersongoncalves.hubdevmanager.service.HubDevDetectorService
import java.awt.event.MouseEvent
import javax.swing.Icon

class HubDevStatusBarWidgetFactory : StatusBarWidgetFactory {

    override fun getId(): String = WIDGET_ID

    override fun getDisplayName(): String = "HubDev Manager"

    override fun isAvailable(project: Project): Boolean {
        return HubDevDetectorService.getInstance().isHubDevInstalled()
    }

    override fun createWidget(project: Project): StatusBarWidget {
        return HubDevStatusBarWidget(project)
    }

    override fun disposeWidget(widget: StatusBarWidget) {
        // nothing to dispose
    }

    companion object {
        const val WIDGET_ID = "HubDevStatusBarWidget"
    }
}

class HubDevStatusBarWidget(private val project: Project) : StatusBarWidget, StatusBarWidget.IconPresentation {

    private var statusBar: StatusBar? = null

    init {
        project.messageBus.connect().subscribe(
            HubDevConfigService.CONFIG_CHANGED,
            object : HubDevConfigService.ConfigChangeListener {
                override fun onConfigChanged() {
                    ApplicationManager.getApplication().invokeLater {
                        statusBar?.updateWidget(ID())
                    }
                }
            }
        )
    }

    override fun ID(): String = HubDevStatusBarWidgetFactory.WIDGET_ID

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun getTooltipText(): String {
        val configService = HubDevConfigService.getInstance(project)
        return if (configService.isLinked) {
            configService.getSiteUrl() ?: "HubDev: Linked"
        } else {
            "HubDev: Click to configure"
        }
    }

    override fun getIcon(): Icon {
        val configService = HubDevConfigService.getInstance(project)
        return if (configService.isLinked) HubDevIcons.LINKED else HubDevIcons.UNLINKED
    }

    override fun getClickConsumer(): Consumer<MouseEvent> {
        return Consumer {
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("HubDev Manager")
            toolWindow?.show()
        }
    }

    override fun dispose() {
        statusBar = null
    }
}
