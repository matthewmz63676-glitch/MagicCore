# MagicCore

MagicCore is a configurable Java 21 Paper/Folia SMP core. The `1.0.0-rc1` build contains the Phase 0–7 architecture and backends: profiles, economy, ranks/capabilities, teams, rewards, survival and marketplace services, Lifesteal/combat, crates/display/store integrations, expansion modules, secure inventory GUIs, KOTH, and verified-vote pinata parties.

```shell
./gradlew clean check releaseBundle
```

Outputs:

- `build/libs/MagicCore-1.0.0-rc1.jar` — shaded Paper/Folia plugin.
- `discord-bot/build/libs/discord-bot.jar` — separately deployable JDA bridge bot.
- `../MagicCore-1.0.0-rc1-source.zip` — sanitized source/config/docs bundle.
- `../MagicCore-1.0.0-rc1-release.zip` — plugin, bot, and operator documentation.

Start with [Phase 0–1](docs/PHASE_0_1.md), then see the phase documents through [Phase 7](docs/PHASE_7.md). [FEATURE_AUDIT.md](docs/FEATURE_AUDIT.md) is the authoritative catalog coverage report and deliberately distinguishes implemented, partial, superseded, and deferred items.

MagicCore's only cheat-detection integration is an optional, bounded, observe-only Vulcan flag feed used as supporting evidence for SpawnStash and Fast Crystal diagnostics. It never changes Vulcan decisions or performs automatic punishment.

Unit/integration tests exercise local SQLite and in-memory contracts. Live MariaDB, MongoDB, Paper/Folia, and optional-plugin staging remain environment-gated and must be completed before promoting the release candidate to a production marketplace build.
