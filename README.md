# HubDev Manager

![HubDev Manager](banners/hubdev-manager.png)

[![JetBrains Marketplace](https://img.shields.io/jetbrains/plugin/v/32158-hubdev-manager.svg)](https://plugins.jetbrains.com/plugin/32158-hubdev-manager)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/32158-hubdev-manager.svg)](https://plugins.jetbrains.com/plugin/32158-hubdev-manager)
[![Build](https://github.com/jeffersongoncalves/hubdev-manager-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/jeffersongoncalves/hubdev-manager-plugin/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/jeffersongoncalves/hubdev-manager-plugin.svg)](https://github.com/jeffersongoncalves/hubdev-manager-plugin/releases)
[![License](https://img.shields.io/github/license/jeffersongoncalves/hubdev-manager-plugin.svg)](LICENSE)

> Manage your HubDev local development sites directly from PhpStorm.

**HubDev Manager** is a JetBrains plugin that integrates [HubDev](https://hubdev.io) into your IDE, letting you link, start, secure, and open sites from the central `sites.yml` without leaving your editor.

- **Homepage**: [GitHub](https://github.com/jeffersongoncalves/hubdev-manager-plugin)
- **Marketplace**: [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/32158-hubdev-manager)
- **Issues**: [GitHub Issues](https://github.com/jeffersongoncalves/hubdev-manager-plugin/issues)

Built as a sibling of `herd-manager-plugin`, adapted to HubDev's model: a single central `~/.devhub/config/sites.yml` (instead of per-project `herd.yml`) and the `hubdev` CLI.

## Features

- **Auto-detection** — Finds the HubDev install on Windows, macOS, and Linux. On Windows it accepts both the new brand name `hubdev` and the legacy executable `devhub.exe` (`C:\Program Files\HubDev\HubDev\devhub.exe`).
- **Site status** — Reads `~/.devhub/config/sites.yml` and shows the current project's domain, PHP version, database, mode (traditional/docker), and active state.
- **Link / Unlink** — `hubdev site:link <path> [domain]` / `site:unlink <name>`.
- **Start / Stop** — `hubdev site:start` / `site:stop` (Caddy route toggle).
- **Renew SSL** — `hubdev site:secure`.
- **Open in Browser** — Opens `https://<domain>`.
- **Open HubDev App** — Launches the GUI.
- **Status bar widget** + **tool window** + **on-open notification**, all reactive to external `sites.yml` changes.

## Requirements

- **IDE**: PhpStorm 2024.3+ (or any IntelliJ-based IDE with the PHP plugin, builds 243–263.*)
- **HubDev**: Installed and available in system PATH or default location
- **Java**: JDK 17+

## Installation

### From JetBrains Marketplace

1. Open PhpStorm → **Settings** → **Plugins** → **Marketplace**
2. Search for **"HubDev Manager"**
3. Click **Install** and restart the IDE

Or install directly from: [HubDev Manager on JetBrains Marketplace](https://plugins.jetbrains.com/plugin/32158-hubdev-manager)

### From Disk

1. Download the latest release `.zip` from [Releases](https://github.com/jeffersongoncalves/hubdev-manager-plugin/releases)
2. Open PhpStorm → **Settings** → **Plugins** → **⚙️** → **Install Plugin from Disk...**
3. Select the `.zip` file and restart the IDE

## Usage

### Quick Start

1. Open a Laravel project in PhpStorm
2. The plugin auto-detects HubDev and checks if the project is registered
3. If not linked, a notification offers to **Link Now**
4. Click **Link Site** to register it with HubDev

### Tool Window

Access via **View → Tool Windows → HubDev Manager** or click the HubDev icon in the right sidebar. The panel shows live site status and the link/start/secure actions.

### Actions Menu

Available under **Tools → HubDev**:

- **Manage Site** — Open the HubDev Manager tool window
- **Link Site** — Link the current project to HubDev
- **Open in Browser** — Open the site URL in your default browser

### Status Bar

The status bar widget (bottom-right) shows link status at a glance and refreshes when `sites.yml` changes externally.

## How detection works

`HubDevDetectorService`:

1. Windows: `%ProgramFiles%\HubDev\HubDev\devhub.exe`, then `hubdev`/`devhub` on PATH.
2. macOS/Linux: common bin dirs + `~/.devhub/bin`, then PATH.
3. Config home: `~/.devhub` (sites in `config/sites.yml`, PHP runtimes in `php/<version>/`).

A project is considered **linked** when its base path matches a site's `path` in `sites.yml` (path comparison normalises slashes, case, and trailing separators).

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

> The Windows `devhub.exe` is a console+GUI hybrid: it only writes CLI output when attached to a real console (PowerShell), not to a Git Bash pty. The IDE's `GeneralCommandLine` uses native pipes, so output is captured correctly.

## Building from Source

Requires a JDK 17 (the IntelliJ Gradle plugin toolchain target).

```bash
# Clone the repository
git clone git@github.com:jeffersongoncalves/hubdev-manager-plugin.git
cd hubdev-manager-plugin

# JAVA_HOME must point at a full JDK 17
./gradlew test          # run unit tests
./gradlew buildPlugin   # produce build/distributions/*.zip
./gradlew runIde        # launch a sandbox PhpStorm with the plugin
```

The built plugin archive will be in `build/distributions/`.

## License

[MIT](LICENSE)

## Author

**Jefferson Goncalves** — [GitHub](https://github.com/jeffersongoncalves)
