# Phase 4 — Lifesteal and combat

> Historical phase gate. Later-phase deferrals below describe the state at this gate; use `FEATURE_AUDIT.md` for current coverage.

## Catalog coverage

- **136–138:** `PersistentLifestealService` owns configurable starting/minimum/maximum hearts, verified player-kill transfers, and `IGNORE`/`LOSE_HEART` environment-death policy.
- **139–141:** Heart withdrawal and overflow enqueue a protected PDC-marked item through the durable delivery mailbox. Consumption enforces maximum hearts. Heart material, strict MiniMessage name, shaped recipe, and ingredients are validated configuration.
- **142–143:** Final-heart elimination supports `SPECTATOR` or `KICK`. Optional revival supports a protected revival item, separately configurable recipe, admin workflow, revival heart count, and committed `PlayerRevived` event.
- **144 and 153:** Unordered killer/victim pairs have durable same-player cooldown records. Server-created `VerifiedPlayerKill` IDs are shared by heart and bounty reward paths and remain idempotent across retries.
- **146–150:** Lock-free combat tags cover both players, expire by configured duration, restrict configured commands/items, resolve logout to one credited kill, and independently enforce pearl/trident cooldowns.
- **151:** New-player protection derives from authoritative profile `firstSeen`, fails closed while loading, supports voluntary removal, and can persist removal on the first attempted attack before allowing later PvP.
- **152:** PvP checks team friendly-fire state and both WorldGuard/claim protection bridges. Unknown or failed provider/cache decisions deny damage. All central teleports reject combat-tagged players and continue to revalidate destination protection on the destination region.

## Platform and exploit boundaries

- Bukkit/Paper player and world mutations go through the scheduler facade or already-owned event thread. No raw Bukkit scheduler was introduced.
- The death listener produces one verified kill shared between hearts and bounties. Combat logout suppresses the synchronous death event and clears its marker immediately, preventing duplicate reward resolution or cross-session marker leakage.
- Team membership events invalidate every online relation cache entry before asynchronous refresh, so friendly-fire decisions fail closed during changes.
- Heart withdrawal commits account mutation and mailbox issuance atomically. Failed item consumption restores the removed item; successful mutation is idempotent by operation key.
- PlaceholderAPI-facing heart state is cached; combat/newbie reads are in-memory and non-blocking.

## Commands and placeholders

- `/magic hearts <status|top|withdraw|revive player>`
- `/magic combat <status|protection remove>`
- `%magiccore_lifesteal_hearts%`, `%magiccore_lifesteal_eliminated%`
- `%magiccore_combat_tagged%`, `%magiccore_combat_remaining_seconds%`, `%magiccore_combat_newbie_protected%`

## Verification and deferrals

`./gradlew clean check` passes with 65 discovered tests: 63 executed and the credential-gated live MariaDB/MongoDB conformance tests skipped. Focused tests prove kill replay protection, durable pair anti-farming, final-heart elimination/revival, overflow/withdrawal mailbox delivery, maximum-heart enforcement, tag expiry/logout credit, and independent ability cooldown behavior.

Live Paper/Folia staging, live WorldGuard/GriefPrevention behavior, and external database conformance remain environment-gated. Revenge-thread bonuses (catalog 145) are explicitly later. Phase 5 crates/display/store integrations, Phase 6 expansions, and Phase 7 events/polished GUI layouts remain deferred.
