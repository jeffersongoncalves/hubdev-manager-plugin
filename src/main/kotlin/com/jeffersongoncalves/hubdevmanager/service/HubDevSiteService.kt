package com.jeffersongoncalves.hubdevmanager.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.Topic
import com.jeffersongoncalves.hubdevmanager.model.HubDevSite

/**
 * Per-project view of HubDev state: whether the open project is registered as a
 * site, and which site that is. Re-reads the central sites.yml when it changes
 * on disk and broadcasts [SITE_CHANGED].
 */
@Service(Service.Level.PROJECT)
class HubDevSiteService(private val project: Project) {

    var site: HubDevSite? = null
        private set

    /** True when this project's directory is registered in sites.yml. */
    val isLinked: Boolean
        get() = site != null

    init {
        val sitesPath = HubDevDetectorService.getInstance().getSitesFilePath()?.toString()
            ?.replace('\\', '/')

        if (sitesPath != null) {
            project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    val changed = events.any { it.path.replace('\\', '/').equals(sitesPath, ignoreCase = true) }
                    if (changed) reload()
                }
            })
        }

        reload()
    }

    fun reload() {
        site = HubDevDetectorService.getInstance().findSiteByPath(project.basePath)
        fireChanged()
    }

    fun siteUrl(): String? = site?.url()

    /** Site name to use when linking this project (existing or derived default). */
    fun suggestedName(): String =
        site?.name ?: HubDevSite.defaultName(project.name)

    private fun fireChanged() {
        project.messageBus.syncPublisher(SITE_CHANGED).onSiteChanged()
    }

    interface SiteChangeListener {
        fun onSiteChanged()
    }

    companion object {
        @JvmField
        val SITE_CHANGED: Topic<SiteChangeListener> = Topic.create(
            "HubDevSiteChanged",
            SiteChangeListener::class.java,
        )

        fun getInstance(project: Project): HubDevSiteService =
            project.getService(HubDevSiteService::class.java)
    }
}
