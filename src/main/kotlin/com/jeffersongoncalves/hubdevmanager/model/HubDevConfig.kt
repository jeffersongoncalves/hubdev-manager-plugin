package com.jeffersongoncalves.hubdevmanager.model

import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

data class DatabaseConfig(
    var driver: String = "mysql",
    var name: String = "",
    var user: String = "root",
    var password: String = "",
)

data class HubDevConfig(
    var name: String = "",
    var domain: String = "",
    var php: String = "8.4",
    var database: DatabaseConfig = DatabaseConfig(),
    var scripts: MutableMap<String, String> = mutableMapOf(),
) {
    fun toDevhubJson(): String = buildString {
        appendLine("{")
        appendLine("  \"php\": \"$php\",")
        appendLine("  \"domain\": \"$domain\",")
        appendLine("  \"database\": \"${database.name}\"")
        append("}")
    }

    fun toYaml(): String = buildString {
        appendLine("# HubDev Team Configuration")
        appendLine("# Share this file with your team so everyone gets the same dev environment.")
        appendLine("# Docs: https://hubdev.io")
        appendLine()
        appendLine("name: $name")
        appendLine("domain: $domain")
        appendLine("php: \"$php\"")
        appendLine("database:")
        appendLine("    driver: ${database.driver}")
        appendLine("    name: ${database.name}")
        appendLine("    user: ${database.user}")
        appendLine("    password: ${database.password}")
        if (scripts.isNotEmpty()) {
            appendLine("scripts:")
            for ((key, value) in scripts) {
                appendLine("    $key: $value")
            }
        }
    }.trimEnd()

    companion object {
        fun fromYaml(content: String): HubDevConfig {
            if (content.isBlank()) return HubDevConfig()

            val yaml = Yaml(SafeConstructor(LoaderOptions()))
            val data = yaml.load<Map<String, Any>>(content) ?: return HubDevConfig()

            val dbMap = (data["database"] as? Map<*, *>)
            val dbConfig = DatabaseConfig(
                driver = dbMap?.get("driver")?.toString() ?: "mysql",
                name = dbMap?.get("name")?.toString() ?: "",
                user = dbMap?.get("user")?.toString() ?: "root",
                password = dbMap?.get("password")?.toString() ?: "",
            )

            val scriptsRaw = (data["scripts"] as? Map<*, *>)
            val scriptsMap = mutableMapOf<String, String>()
            scriptsRaw?.forEach { (k, v) ->
                if (k != null && v != null) {
                    scriptsMap[k.toString()] = v.toString()
                }
            }

            return HubDevConfig(
                name = data["name"]?.toString() ?: "",
                domain = data["domain"]?.toString() ?: "",
                php = data["php"]?.toString() ?: "8.4",
                database = dbConfig,
                scripts = scriptsMap,
            )
        }

        fun createDefault(projectName: String): HubDevConfig {
            val sanitized = projectName.lowercase().replace(Regex("[^a-z0-9-]"), "-")
            val dbName = sanitized.replace("-", "_")
            return HubDevConfig(
                name = sanitized,
                domain = "$sanitized.test",
                php = "8.4",
                database = DatabaseConfig(
                    driver = "mysql",
                    name = dbName,
                    user = "root",
                    password = "",
                ),
                scripts = mutableMapOf(
                    "setup" to "composer install && npm install",
                    "build" to "npm run build",
                    "migrate" to "php artisan migrate --seed",
                ),
            )
        }
    }
}
