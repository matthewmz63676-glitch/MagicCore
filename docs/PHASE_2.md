# Phase 2 — Survival essentials

> Historical phase gate. Later-phase deferrals below describe the state at this gate; use `FEATURE_AUDIT.md` for current coverage.

## Implemented

- UUID-owned homes with rank limits, safe names, sharing, visibility, deletion, idempotency, and durable locations.
- Public/access-controlled server warps plus a capability-gated spawn mutation.
- Player warps with rank limits, categories, ownership, protection validation, active state, visits, and idempotent mutations. Phase 6 will add favorites, ratings, moderation workflows, sponsorship, and search view models.
- TPA/TPAHere request, accept, deny, cancel, expiry, and persisted opt-out settings.
- Folia-safe teleport coordination: player work uses entity schedulers, candidate checks use region schedulers, I/O remains asynchronous, and cross-region operations use immutable destination messages without held locks.
- Back/death-back persistence, movement-cancelled warmups, bounded RTP candidates, terrain safety checks, world-border checks, and protection checks.
- Configured kits with capability access, cooldowns, idempotent claims, and durable mailbox delivery.
- Player settings for teleport requests, messages, mentions, scoreboard, phantoms, and team chat.
- Configured shop categories/products with exact integer prices, atomic debit plus durable purchase delivery, quantity selection, and economy-ledger receipts.
- Selling with SHA-256 fingerprints over normalized exact item bytes, quote expiry, pre-removal reservation, entity-thread fingerprint/quantity revalidation, durable recovery payloads, and atomic credit/settlement.
- Inventory delivery preflight and persistent delivery markers. Full inventories retain pending deliveries; retries recognize already-inserted deliveries rather than silently dropping or duplicating them.
- Optional WorldGuard 7 adapter isolated behind a soft-dependency bridge. A configured missing provider is explicit and fail-closed; selecting no provider is explicit allow-all behavior.
- Internal placeholders and optional PlaceholderAPI exposure for home count, TPA state, player-warp count, and shop product count.
- `/magic` subcommands for homes, warps, spawn, TPA, back, RTP, kits, settings, shop buying/selling, and player warps. MagicCore does not claim `/buy`.

## Configuration

- `modules/essentials.yml`: home names, teleport timing, RTP bounds/attempts, and kits.
- `modules/shop.yml`: currency and stable product definitions.
- `modules/settings.yml`: documented setting defaults.
- `integrations.yml`: WorldGuard and claim-provider selections.

These files are typed, installed as defaults, validated before startup, included in reload change detection, and require restart when gameplay values change.

## Verification and limits

`./gradlew clean check` passes with 56 tests: 54 executed and 2 environment-gated live-database tests skipped. The suite covers idempotent/atomic shop purchase, sell reservation before removal, paid-teleport refund/cooldown behavior, stale request expiry, settings opt-out, teleport movement cancellation, RTP bounds, kit cooldown/delivery, player-warp limits/visits, existing Phase 0/1 behavior, and the no-raw-scheduler architecture guard.

Live Paper and Folia server smoke tests, and live MariaDB/MongoDB conformance, still require external server/database environments. Compilation and unit tests are not presented as substitutes for those runtime checks.

GUI layouts remain intentionally deferred to Phase 7. When added, they will be thin clients over these services and follow the supplied Minecraft GUI style guide; they will not own balances, authorization, fingerprints, protection state, or transactions.
