package com.jeffersongoncalves.hubdevmanager.icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object HubDevIcons {
    @JvmField
    val HUBDEV: Icon = IconLoader.getIcon("/icons/hubdev.svg", HubDevIcons::class.java)

    @JvmField
    val LINKED: Icon = IconLoader.getIcon("/icons/hubdev_linked.svg", HubDevIcons::class.java)

    @JvmField
    val UNLINKED: Icon = IconLoader.getIcon("/icons/hubdev_unlinked.svg", HubDevIcons::class.java)
}
