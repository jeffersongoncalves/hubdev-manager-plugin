package com.jeffersongoncalves.hubdevmanager

import com.jeffersongoncalves.hubdevmanager.model.HubDevSite
import org.junit.Assert.*
import org.junit.Test

class HubDevSiteTest {

    private val sample = """
        sites:
            - name: app-state
              domain: app-state.test
              path: C:\PROJETOS\Anteloope\app-state
              doc_root: C:\PROJETOS\Anteloope\app-state\public
              driver: laravel
              php_version: "8.4"
              database: app_state
              active: true
              mode: traditional
            - name: filakit
              domain: filakit.test
              path: C:\PROJETOS\startkit\filakit
              doc_root: C:\PROJETOS\startkit\filakit\public
              driver: laravel
              php_version: "8.3"
              database: filakit
              active: false
              mode: docker
    """.trimIndent()

    @Test
    fun `parseAll reads every site`() {
        val sites = HubDevSite.parseAll(sample)
        assertEquals(2, sites.size)
    }

    @Test
    fun `parseAll maps all fields`() {
        val site = HubDevSite.parseAll(sample).first()
        assertEquals("app-state", site.name)
        assertEquals("app-state.test", site.domain)
        assertEquals("C:\\PROJETOS\\Anteloope\\app-state", site.path)
        assertEquals("8.4", site.phpVersion)
        assertEquals("app_state", site.database)
        assertTrue(site.active)
        assertEquals("traditional", site.mode)
    }

    @Test
    fun `parseAll handles inactive docker site`() {
        val site = HubDevSite.parseAll(sample)[1]
        assertEquals("8.3", site.phpVersion)
        assertFalse(site.active)
        assertEquals("docker", site.mode)
    }

    @Test
    fun `url uses https with the site domain`() {
        val site = HubDevSite.parseAll(sample).first()
        assertEquals("https://app-state.test", site.url())
    }

    @Test
    fun `parseAll returns empty list on blank input`() {
        assertTrue(HubDevSite.parseAll("").isEmpty())
    }

    @Test
    fun `parseAll returns empty list when sites key missing`() {
        assertTrue(HubDevSite.parseAll("other: value").isEmpty())
    }

    @Test
    fun `normalizePath equates windows slashes and case and trailing separators`() {
        assertEquals(
            HubDevSite.normalizePath("C:\\PROJETOS\\App\\"),
            HubDevSite.normalizePath("c:/projetos/app"),
        )
    }

    @Test
    fun `defaultName kebab-cases project name`() {
        assertEquals("my-project-name", HubDevSite.defaultName("My Project Name"))
        assertEquals("my-app-v2", HubDevSite.defaultName("my_app.v2"))
    }
}
