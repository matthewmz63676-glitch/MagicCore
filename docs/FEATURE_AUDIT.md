# Feature catalog audit — 1 through 248

This audit uses the integration pack's `FEATURE_CATALOG.md` as the numbering authority. “Implemented” means a typed service/controller/config path exists and has automated contract coverage where practical. It does not mean live external-plugin behavior was staged.

| Catalog IDs | Status | Evidence or boundary |
| --- | --- | --- |
| 1–40 | Implemented | Module lifecycle/provider graph, registries, storage/config/audit/export/import, rank modes/capabilities/limits, and target scope. |
| 41–46 | Capability/catalog only | Staff chat, vanish, fly-speed runtime, gamemode, heal/god, and a dedicated punishment-history UI are not shipped as MagicCore-owned utility modules. The stable capabilities exist for provider/admin use. |
| 47–52, 54–60 | Implemented | Internal/Vault economy, currencies, balances, audited adjustments, immutable ledger, analytics/snapshots, exact math, pagination, and imports. |
| 53 | Partial | Atomic player transfer exists; a two-step player payment confirmation UX is not shipped. |
| 61–67, 69–72 | Implemented | Team lifecycle, policy, invitations/roles/limits, teleport policy hooks, display, friendly fire, and statistics hooks. |
| 68, 73 | Not shipped | Team-home ownership and an external team provider mode remain adapter extensions. |
| 74–88, 90 | Implemented | Essentials, safety/warmups, kits, AFK/status, settings, greetings/death hooks, and recovery-safe grants. |
| 89 | Partial | Secure ender chest and selected convenience behavior exist; MagicCore does not own the full listed convenience-command suite. |
| 91 | Not shipped | Essentials remains internal; no external essentials provider adapter is declared. |
| 92–94, 98–111 | Implemented | Shop/sell, marketplace, escrow, recovery, histories, diagnostics, and serialization boundaries. |
| 95–96 | Partial | Quantity and quote paths exist; not every buy/product path has a separate confirmation or capability field. |
| 97, 112 | Not shipped | Dynamic stock/tax/discount rules and external shop/auction ownership modes are not in this release candidate. |
| 113, 115–117, 121–124 | Implemented | Daily/playtime persistence, weighted claims, preview pool, streak state, milestone policy, idempotency, and trusted vote ingestion. |
| 114 | Partial | `choices` is configurable but the current claim service selects one authoritative weighted result rather than presenting multiple selectable rolls. |
| 118–120 | Not shipped | Daily streak milestone rewards, ready notifications, and a dedicated reward reset/debug mutation are not implemented. |
| 125–126 | Partial | Offline votes are durably counted/eligible for pinata rewards and public events; per-vote item rewards and per-player vote streak/cooldown statistics are not implemented. |
| 127 | Not shipped | Vote-site definitions are normalized from provider service names, not configured as a site catalog. |
| 128–133, 135 | Implemented | Internal/external crates, costs, pools, multi-open, atomic delivery, milestones, and provider mode. |
| 134 | Deferred by catalog | Crate opening animation remains deliberately unimplemented. |
| 136–144, 146–164 | Implemented | Lifesteal, combat, protection, bounties, display, remembered toggles, and cached leaderboards. |
| 145 | Deferred by catalog | Revenge-thread bonuses remain deliberately unimplemented. |
| 165 | Partial | Mentions/settings hooks exist; MagicCore does not install a general chat-formatting owner. |
| 166–178 | Implemented | TAB/PAPI/store/DiscordSRV/hologram/NPC/custom-item/Bedrock/stacking integration boundaries. |
| 179 | Superseded by user direction | Only bounded, observe-only Vulcan evidence capture remains. |
| 180–205 | Implemented | KOTH, vote party/pinata, announcements/maintenance, provider-independent placeholders, Folia contexts, storage providers/tests, Apollo, presentation, AFK shards, and settings. |
| 206 | Partial | Public profile/privacy/admin views ship, but the public view does not aggregate every optional team/economy/heart/vote/reward/warp field into one record. |
| 207–236, 238–240 | Implemented | Privacy/admin links, resets, Fast Crystal, SpawnStash, worth/sell, barter/tools, keyalls, Gems/GemShop, and storage safety/recovery. |
| 237 | Partial | Secure ender-chest self-service and capability-gated cross-owner backend inspection exist; a dedicated staff inspection GUI flow is not exposed. |
| 241–243 | Implemented | Separate bot, linking, signatures, replay/rate/retry/health. |
| 244 | Partial | Bridge commands/outbox/moderation transport exist; automatic Discord role synchronization is not enabled by default. |
| 245–248 | Implemented | PlayerWarp ownership/safety/moderation/expiry, searchable/favorite/promoted views, sponsorship atomicity/caps/order/expiry/refunds. |

The release candidate therefore completes the blueprint's Phase 0–7 module scope while retaining explicit catalog deferrals and partials instead of representing all 248 aspirations as finished. Production promotion also remains contingent on the live staging matrix in `PHASE_7.md`.
