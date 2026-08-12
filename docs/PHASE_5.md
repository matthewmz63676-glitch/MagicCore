# Phase 5 — Crates, display, store, and integrations

> Historical phase gate. Later-phase deferrals below describe the state at this gate; use `FEATURE_AUDIT.md` for current coverage.

## Catalog coverage

- **128–133:** Typed native crate/key definitions support weighted rarity pools, previews, key or currency costs, bounded multiple-open amounts, one-transaction payment/reward settlement, durable item mailbox delivery, operation replay protection, and threshold milestones. Catalog item 134 explicitly labels opening animations as later work.
- **135:** `EXTERNAL` crate ownership binds to the public ExcellentCrates API for crate/key lookup and provider-owned opening flow. Every grant/open is checkpointed as an external saga; an ambiguous prepared replay stops with reconciliation required rather than duplicating a side effect. Missing providers and ExcellentCrates-on-Folia fail validation because ExcellentCrates currently documents no Folia support.
- **154–158:** Bounties enforce configured min/max/tax escrow, explicit command confirmation, UUID/name search, sorting, history, stat restrictions, verified-kill atomic claims, and WorldGuard/claim protection. All bounty and contribution reads are keyset-paged rather than capped at 1,000 records.
- **159–165:** The internal display provider supplies configurable scoreboard, tab list, rank ordering/name formatting, below-name health, remembered scoreboard toggle, cached kills/deaths/playtime/hearts/wealth leaderboards, and optional chat/mention presentation.
- **166–167:** TAB is an explicit external display mode. MagicCore's validated internal placeholder registry remains authoritative and is exposed through the optional PlaceholderAPI expansion.
- **168–172:** `/magic store` exposes link, product, and donation-goal help without claiming `/buy`. Signed HMAC purchase events map only to typed currency/key/item/rank actions, reject stale/invalid/conflicting events, checkpoint each fulfillment action, deliver items through the mailbox, announce exactly once, and advance the persistent goal exactly once.
- **173–178:** Optional adapters cover DiscordSRV linking/notifications, DecentHolograms, Citizens typed click actions, Nexo/ItemsAdder namespaced items, Geyser/Floodgate Bedrock detection, and RoseStacker/WildStacker compatibility. Provider selection is validated and no optional plugin is shaded.
- **179:** The requested Vulcan-only hook observes `VulcanFlagEvent` without changing Vulcan decisions. A bounded per-player flag buffer and typed domain event provide diagnostics and a safe evidence input for the Phase 6 SpawnStash workflow.

## Import and migration framework

CSV imports are constrained to the imports directory and identified by SHA-256 fingerprints. Preview reports mappings and ambiguous rows without mutation. Execute/resume uses persistent job checkpoints and per-row operation keys; reconcile re-verifies target state. Included targets cover profiles, balances, ranks, and crate keys, while source files remain untouched.

## Provider and transaction boundaries

- Native crates require internal economy so payment and rewards share one transaction.
- External crate calls use explicit saga state and never silently fall back to native ownership.
- ExcellentCrates integration uses its typed manager/user/key APIs; no console-command glue is generated.
- External display, Discord, hologram, NPC, custom-item, Bedrock, stacking, and Vulcan hooks are optional and fail/degrade explicitly according to whether mutations could be unsafe.
- ExcellentCrates currently targets Paper/Spigot, not Folia. Native crates remain the supported Folia path.

## Commands and placeholders

- `/magic crate <list|preview|keys|open|history|grant>`
- `/magic stats [top <kills|deaths|playtime>]`
- `/magic leaderboard <kills|deaths|playtime|hearts|wealth> [currency]`
- `/magic store <link|products|goal>`
- `/magic import <preview|execute|reconcile|status>`
- `%magiccore_crates_<key>_keys%`, `%magiccore_crates_<crate>_opens%`
- `%magiccore_stats_kills%`, `%magiccore_stats_deaths%`, `%magiccore_stats_playtime_seconds%`

## Verification and deferrals

Focused tests cover native crate atomicity/milestones/mailbox/replay, external saga ambiguity and replay, store signatures/typed fulfillment/goal announcements, player statistics and leaderboards, importer preview/checkpoint/reconciliation, Vulcan flag bounds, configuration contracts, and more-than-1,000-record bounty search/claim behavior.

The clean phase gate passed with 75 discovered tests: 73 executed, 2 credential-gated live MariaDB/MongoDB tests skipped, and 0 failures/errors. The shaded JAR contains `plugin.yml` and 4,525 entries; inspected LuckPerms, PlaceholderAPI, Vault, WorldGuard, Vulcan, and ExcellentCrates API package counts are all zero. Live Paper/Folia presentation checks and live external-plugin API checks remain environment-gated and are not represented as unit-test proof. Catalog item 134 animations, Phase 6 expansion modules, Phase 7 events, and polished inventory GUIs remain deferred.
