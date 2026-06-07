# HubDev Manager — PhpStorm Plugin

Manage your [HubDev](https://hubdev.io) local development sites directly from PhpStorm.

Built as a sibling of `herd-manager-plugin`, adapted to HubDev's model: a single
central `~/.devhub/config/sites.yml` (instead of per-project `herd.yml`) and the
`hubdev` CLI.

## Features

- **Auto-detection** — finds the HubDev install on Windows, macOS, and Linux.
  On Windows it accepts both the new brand name `hubdev` and the legacy
  executable `devhub.exe` (`C:\Program Files\HubDev\HubDev\devhub.exe`).
- **Site status** — reads `~/.devhub/config/sites.yml` and shows the current
  project's domain, PHP version, database, mode (traditional/docker), and active
  state.
- **Link / Unlink** — `hubdev site:link <path> [domain]` / `site:unlink <name>`.
- **Start / Stop** — `hubdev site:start` / `site:stop` (Caddy route toggle).
- **Renew SSL** — `hubdev site:secure`.
- **Open in Browser** — opens `https://<domain>`.
- **Open HubDev App** — launches the GUI.
- **Status bar widget** + **tool window** + **on-open notification**, all
  reactive to external `sites.yml` changes.

## How detection works

`HubDevDetectorService`:

1. Windows: `%ProgramFiles%\HubDev\HubDev\devhub.exe`, then `hubdev`/`devhub` on PATH.
2. macOS/Linux: common bin dirs + `~/.devhub/bin`, then PATH.
3. Config home: `~/.devhub` (sites in `config/sites.yml`, PHP runtimes in `php/<version>/`).

A project is considered **linked** when its base path matches a site's `path`
in `sites.yml` (path comparison normalises slashes, case, and trailing separators).

## CLI commands used

Discovered from `hubdev --help` (HubDev v1.19.0):

| Action        | Command                          |
|---------------|----------------------------------|
| Link          | `site:link <path> [domain]`      |
| Unlink        | `site:unlink <name>`             |
| Start         | `site:start <name>`              |
| Stop          | `site:stop <name>`               |
| Renew SSL     | `site:secure`                    |
| Open          | `site:open <domain>`             |
| List sites    | `site:list`                      |
| PHP versions  | `php:list`                       |

> The Windows `devhub.exe` is a console+GUI hybrid: it only writes CLI output
> when attached to a real console (PowerShell), not to a Git Bash pty. The IDE's
> `GeneralCommandLine` uses native pipes, so output is captured correctly.

## Build

Requires a JDK 17 (the IntelliJ Gradle plugin toolchain target).

```bash
# JAVA_HOME must point at a full JDK 17
./gradlew test          # run unit tests
./gradlew buildPlugin   # produce build/distributions/*.zip
./gradlew runIde        # launch a sandbox PhpStorm with the plugin
```

## Project layout

```
src/main/kotlin/com/jeffersongoncalves/hubdevmanager/
  HubDevManagerBundle.kt
  model/HubDevSite.kt                  # sites.yml entry + parser
  service/HubDevDetectorService.kt     # install detection + config reading
  service/HubDevCliService.kt          # runs the hubdev CLI
  service/HubDevSiteService.kt         # per-project link status (project service)
  ui/HubDevToolWindowFactory.kt
  ui/HubDevToolWindowPanel.kt
  ui/HubDevStatusBarWidgetFactory.kt
  actions/HubDevManageSiteAction.kt
  actions/HubDevLinkSiteAction.kt
  actions/HubDevOpenInBrowserAction.kt
  listeners/HubDevProjectOpenListener.kt
  icons/HubDevIcons.kt
```
