# Phase 6 — Expansion modules

Phase 6 implements catalog 196–248 as service-owned modules with configuration under `modules/`, typed persistence, capability checks at mutation time, and command/GUI clients that do not own business state.

Implemented areas include:

- Apollo Lunar capability detection with vanilla fallback; data-driven info/server/application views; AFK shard policy, ledger, caps, anti-farm signals, settings, profiles, privacy, and administrative profile views.
- Dry-run and resumable statistics resets; server-authoritative Fast Crystal behavior; protected, observe-only SpawnStash evidence cases with crash-resumable restoration.
- Item-worth policy, quote/fingerprint/reconciliation selling, Billford barter, configured area tools, auto Keyall, Gems/GemShop, secure vault and ender-chest persistence, and categorized/sponsored PlayerWarps.
- A separate custom Discord bot bridge with expiring one-time codes, HMAC envelopes, nonce replay protection, rate limits, retries, and health reporting.
- Vulcan-only bounded flag correlation for SpawnStash and Fast Crystal. Flag evidence is informational and cannot approve, deny, cancel, or punish gameplay.

The secure-storage GUI added during Phase 7 consumes the Phase 6 backend. It uses one exclusive lease, immutable revisions, CAS commits, insertion byte budgets, explicit nested/custom-item policy, and recovery-mailbox fallback. Shift-click, hotbar swaps, offhand swaps, drags, stale revisions, expired leases, and duplicate saves are covered by boundary or service tests.

Live custom-item, Apollo, Discord, WorldGuard/claim, Vulcan, and multi-server database staging is not represented by unit-test success.
