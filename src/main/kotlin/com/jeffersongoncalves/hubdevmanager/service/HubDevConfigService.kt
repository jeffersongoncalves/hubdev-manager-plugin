package com.jeffersongoncalves.hubdevmanager.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.Topic
import com.jeffersongoncalves.hubdevmanager.model.HubDevConfig

@Service(Service.Level.PROJECT)
class HubDevConfigService(private val project: Project) {

    private val log = Logger.getInstance(HubDevConfigService::class.java)

    var config: HubDevConfig? = null
        private set

    var isLinked: Boolean = false
        private set

    init {
        project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                val projectBase = project.basePath ?: return
                for (event in events) {
                    val path = event.path ?: continue
                    if (!path.replace("\\", "/").startsWith(projectBase.replace("\\", "/"))) continue
                    val fileName = path.substringAfterLast("/").substringAfterLast("\\")
                    if (fileName != "devhub.yml") continue

                    when (event) {
                        is VFileCreateEvent, is VFileContentChangeEvent -> loadConfig()
                        is VFileDeleteEvent -> {
                            config = null
                            isLinked = false
                            fireConfigChanged()
                        }
                    }
                }
            }
        })

        loadConfig()
    }

    fun loadConfig() {
        val devhubYml = findDevhubYml()
        if (devhubYml != null) {
            try {
                val content = String(devhubYml.contentsToByteArray(), Charsets.UTF_8)
                config = HubDevConfig.fromYaml(content)
            } catch (e: Exception) {
                log.warn("Failed to parse devhub.yml", e)
                config = null
            }
        } else {
            config = null
        }

        checkLinkStatus()
        fireConfigChanged()
    }

    fun saveConfig(newConfig: HubDevConfig) {
        WriteAction.run<Exception> {
            val baseDir = ProjectRootManager.getInstance(project).contentRoots.firstOrNull() ?: return@run
            val file = baseDir.findChild("devhub.yml") ?: baseDir.createChildData(this, "devhub.yml")
            VfsUtil.saveText(file, newConfig.toYaml())
        }
        config = newConfig
        checkLinkStatus()
        fireConfigChanged()
    }

    fun hasConfig(): Boolean = config != null

    fun getSiteUrl(): String? {
        val cfg = config ?: return null
        if (cfg.domain.isBlank()) return null
        return "http://${cfg.domain}"
    }

    fun checkLinkStatus() {
        val cfg = config
        isLinked = if (cfg != null && cfg.name.isNotBlank()) {
            HubDevDetectorService.getInstance().isSiteLinked(cfg.name)
        } else {
            false
        }
    }

    fun getDefaultConfig(): HubDevConfig {
        val projectName = project.name
        return HubDevConfig.createDefault(projectName)
    }

    private fun findDevhubYml(): VirtualFile? {
        val baseDir = ProjectRootManager.getInstance(project).contentRoots.firstOrNull() ?: return null
        return baseDir.findChild("devhub.yml")
    }

    private fun fireConfigChanged() {
        project.messageBus.syncPublisher(CONFIG_CHANGED).onConfigChanged()
    }

    interface ConfigChangeListener {
        fun onConfigChanged()
    }

    companion object {
        @JvmField
        val CONFIG_CHANGED: Topic<ConfigChangeListener> = Topic.create(
            "HubDevConfigChanged",
            ConfigChangeListener::class.java
        )

        fun getInstance(project: Project): HubDevConfigService =
            project.getService(HubDevConfigService::class.java)
    }
}
