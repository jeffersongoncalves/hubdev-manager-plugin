package com.jeffersongoncalves.hubdevmanager

import com.jeffersongoncalves.hubdevmanager.model.DatabaseConfig
import com.jeffersongoncalves.hubdevmanager.model.HubDevConfig
import org.junit.Assert.*
import org.junit.Test

class HubDevConfigTest {

    @Test
    fun `fromYaml parses valid devhub yml`() {
        val yaml = """
            name: hubdev-api
            domain: hubdev-api.test
            php: "8.4"
            database:
                driver: mysql
                name: hubdev_api
                user: root
                password: devhub123
            scripts:
                setup: composer install && npm install
                build: npm run build
                migrate: php artisan migrate --seed
        """.trimIndent()

        val config = HubDevConfig.fromYaml(yaml)

        assertEquals("hubdev-api", config.name)
        assertEquals("hubdev-api.test", config.domain)
        assertEquals("8.4", config.php)
        assertEquals("mysql", config.database.driver)
        assertEquals("hubdev_api", config.database.name)
        assertEquals("root", config.database.user)
        assertEquals("devhub123", config.database.password)
        assertEquals(3, config.scripts.size)
        assertEquals("composer install && npm install", config.scripts["setup"])
        assertEquals("npm run build", config.scripts["build"])
        assertEquals("php artisan migrate --seed", config.scripts["migrate"])
    }

    @Test
    fun `fromYaml handles php version without quotes`() {
        val yaml = """
            name: test-site
            domain: test-site.test
            php: 8.3
        """.trimIndent()

        val config = HubDevConfig.fromYaml(yaml)

        assertEquals("test-site", config.name)
        assertEquals("8.3", config.php)
    }

    @Test
    fun `fromYaml handles missing database section`() {
        val yaml = """
            name: test-site
            domain: test-site.test
            php: "8.4"
        """.trimIndent()

        val config = HubDevConfig.fromYaml(yaml)

        assertEquals("mysql", config.database.driver)
        assertEquals("root", config.database.user)
        assertEquals("", config.database.password)
    }

    @Test
    fun `fromYaml handles missing scripts section`() {
        val yaml = """
            name: test-site
            domain: test-site.test
            php: "8.4"
        """.trimIndent()

        val config = HubDevConfig.fromYaml(yaml)

        assertTrue(config.scripts.isEmpty())
    }

    @Test
    fun `fromYaml handles empty content gracefully`() {
        val config = HubDevConfig.fromYaml("")

        assertEquals("", config.name)
        assertEquals("", config.domain)
        assertEquals("8.4", config.php)
    }

    @Test
    fun `toYaml produces correct format with header comment`() {
        val config = HubDevConfig(
            name = "my-app",
            domain = "my-app.test",
            php = "8.4",
            database = DatabaseConfig(
                driver = "mysql",
                name = "my_app",
                user = "root",
                password = "secret",
            ),
            scripts = mutableMapOf(
                "setup" to "composer install",
                "build" to "npm run build",
            ),
        )

        val yaml = config.toYaml()

        assertTrue(yaml.contains("# HubDev Team Configuration"))
        assertTrue(yaml.contains("# Docs: https://hubdev.io"))
        assertTrue(yaml.contains("name: my-app"))
        assertTrue(yaml.contains("domain: my-app.test"))
        assertTrue(yaml.contains("php: \"8.4\""))
        assertTrue(yaml.contains("driver: mysql"))
        assertTrue(yaml.contains("name: my_app"))
        assertTrue(yaml.contains("user: root"))
        assertTrue(yaml.contains("password: secret"))
        assertTrue(yaml.contains("scripts:"))
        assertTrue(yaml.contains("    setup: composer install"))
        assertTrue(yaml.contains("    build: npm run build"))
    }

    @Test
    fun `toYaml roundtrip preserves data`() {
        val original = HubDevConfig(
            name = "roundtrip-test",
            domain = "roundtrip-test.test",
            php = "8.2",
            database = DatabaseConfig(
                driver = "pgsql",
                name = "roundtrip_test",
                user = "admin",
                password = "pass123",
            ),
            scripts = mutableMapOf("setup" to "composer install"),
        )
        val yaml = original.toYaml()
        val parsed = HubDevConfig.fromYaml(yaml)

        assertEquals(original.name, parsed.name)
        assertEquals(original.domain, parsed.domain)
        assertEquals(original.php, parsed.php)
        assertEquals(original.database.driver, parsed.database.driver)
        assertEquals(original.database.name, parsed.database.name)
        assertEquals(original.database.user, parsed.database.user)
        assertEquals(original.database.password, parsed.database.password)
        assertEquals(original.scripts, parsed.scripts)
    }

    @Test
    fun `createDefault generates valid config from project name`() {
        val config = HubDevConfig.createDefault("My Project Name")

        assertEquals("my-project-name", config.name)
        assertEquals("my-project-name.test", config.domain)
        assertEquals("8.4", config.php)
        assertEquals("mysql", config.database.driver)
        assertEquals("my_project_name", config.database.name)
        assertEquals("root", config.database.user)
        assertEquals("", config.database.password)
        assertEquals(3, config.scripts.size)
        assertEquals("composer install && npm install", config.scripts["setup"])
    }

    @Test
    fun `createDefault sanitizes special characters`() {
        val config = HubDevConfig.createDefault("My Project.v2")

        assertEquals("my-project-v2", config.name)
        assertEquals("my-project-v2.test", config.domain)
        assertEquals("my_project_v2", config.database.name)
    }

    @Test
    fun `database config defaults`() {
        val db = DatabaseConfig()

        assertEquals("mysql", db.driver)
        assertEquals("root", db.user)
        assertEquals("", db.password)
    }

    @Test
    fun `scripts map is mutable`() {
        val config = HubDevConfig()

        config.scripts["test"] = "php artisan test"
        assertEquals(1, config.scripts.size)

        config.scripts.remove("test")
        assertTrue(config.scripts.isEmpty())
    }
}
