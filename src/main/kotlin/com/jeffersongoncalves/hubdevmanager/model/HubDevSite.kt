package com.jeffersongoncalves.hubdevmanager.model

import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

/**
 * One registered HubDev site, as stored in ~/.devhub/config/sites.yml.
 *
 * HubDev keeps every site in a single central file (unlike Laravel Herd's
 * per-project herd.yml). A project is "linked" when its directory matches a
 * site's [path].
 */
data class HubDevSite(
    val name: String = "",
    val domain: String = "",
    val path: String = "",
    val docRoot: String = "",
    val driver: String = "laravel",
    val phpVersion: String = "8.4",
    val database: String = "",
    val active: Boolean = true,
    val mode: String = "traditional",
) {
    /** HubDev fronts every .test domain with Caddy + auto TLS, so sites are https. */
    fun url(): String = "https://$domain"

    companion object {
        /** Normalises a Windows/Unix path for equality checks. */
        fun normalizePath(path: String): String =
            path.trim().trimEnd('/', '\\').replace('\\', '/').lowercase()

        /**
         * Parses ~/.devhub/config/sites.yml and returns every registered site.
         * Returns an empty list on any parse error.
         */
        @Suppress("UNCHECKED_CAST")
        fun parseAll(yamlContent: String): List<HubDevSite> {
            return try {
                val yaml = Yaml(SafeConstructor(LoaderOptions()))
                val root = yaml.load<Map<String, Any>>(yamlContent) ?: return emptyList()
                val sites = root["sites"] as? List<Map<String, Any>> ?: return emptyList()
                sites.map { entry ->
                    HubDevSite(
                        name = entry["name"]?.toString() ?: "",
                        domain = entry["domain"]?.toString() ?: "",
                        path = entry["path"]?.toString() ?: "",
                        docRoot = entry["doc_root"]?.toString() ?: "",
                        driver = entry["driver"]?.toString() ?: "laravel",
                        phpVersion = entry["php_version"]?.toString() ?: "8.4",
                        database = entry["database"]?.toString() ?: "",
                        active = entry["active"] as? Boolean ?: true,
                        mode = entry["mode"]?.toString() ?: "traditional",
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

        /** Default site name derived from a project name (kebab-cased). */
        fun defaultName(projectName: String): String =
            projectName.lowercase().replace(Regex("[^a-z0-9-]"), "-").trim('-')
    }
}
