# Phase 7 — Events, menus, and release candidate

Phase 7 implements catalog 180–187 and the release architecture.

## Events

- KOTH definitions configure cuboid regions, first delay, recurring schedule, capture time, banned materials, and typed currency rewards.
- The Folia-safe controller snapshots players on entity schedulers, resolves authoritative team membership through `TeamService`, resets progress when contested, and commits winner rewards with the run transition.
- Vote events are accepted only from the reflected NuVotifier Bukkit event or the capability-gated administrative backend. Provider event IDs are durable and idempotent.
- Offline votes follow `COUNT_AND_REWARD`, `COUNT_ONLY`, or `IGNORE`. Thresholds create persistent queued pinata parties.
- Pinatas configure entity/location, global and per-player hit limits, voter eligibility, hit/final rewards, and a remembered-settings-aware bossbar.
- KOTH/pinata changes publish typed domain events after commit. Announcements respect announcement, sound, particle, and bossbar settings. Scheduled announcements and coarse sponsorship/lease maintenance use the scheduler facade.

## Menus

The shared framework follows the supplied Minecraft GUI style guide: short dark titles, balanced stained-glass layouts, one accent family, stable back/close/previous/next slots, explicit state materials, ordered lore, and no false action labels. Main, info, server navigation, profile, settings, GemShop confirmation, PlayerWarps, selling, events, and secure storage are configured or rendered through typed view models.

All ordinary MagicCore menus cancel top/bottom clicks, shift movement, hotbar paths, outside clicks, and drags. Secure storage is the intentional mutable exception and has a separate lease-aware controller with fail-closed insertion validation.

## Release evidence and remaining staging

The Gradle release task produces the shaded plugin, separate bot, source bundle, and operator bundle. The clean release gate discovered 111 tests: 109 executed, two credential-gated MariaDB/MongoDB tests skipped, and zero failures/errors. The suite includes configuration, architecture, persistence, replay, inventory-dupe, event, and repeated-event soak coverage.

Still required before production promotion: boot and interaction smoke tests on real Paper and Folia servers; live tests with every enabled optional provider; real MariaDB/MongoDB transaction probes; Bedrock interaction checks; and a long-running multi-player performance soak. Compilation and unit tests are not substitutes for those checks.
