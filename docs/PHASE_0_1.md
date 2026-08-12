# Phase 0 and Phase 1

> Historical phase gate. Later phases are now implemented; use `FEATURE_AUDIT.md` for current release coverage.

This document describes the foundation and first vertical slice. Phase 2 is documented separately.

## Runtime architecture

- Java 21, Gradle Kotlin DSL, Paper API 1.21.11, and `folia-supported: true`.
- One modular plugin JAR with explicit module owners and dependency validation.
- Only `PaperFoliaScheduler` touches Paper's global, region, entity, or async schedulers.
- SQL, MongoDB, configuration files, and serialization run through bounded I/O executors.
- Domain services return asynchronous, typed results; modules never dispatch provider commands.
- Storage is a provider-neutral transactional record API with compare-and-swap revisions, unique idempotency keys, pagination, migrations, and rollback.
- SQLite is the default. MariaDB and transaction-capable MongoDB are explicit alternatives. MongoDB fails startup when required transaction guarantees are absent.

## Configuration

The beginner-facing files are:

- `config.yml`: identity, preset, language, safety, and bounded I/O settings.
- `features.yml`: explicit module ownership; unimplemented phases ship as `DISABLED`.
- `storage.yml`: SQLite, MariaDB, or MongoDB with environment-variable secret references.
- `integrations.yml`: LuckPerms, Vault, PlaceholderAPI, and future integration choices.
- `ranks.yml`: stable IDs, exactly five donor examples, exactly five staff examples, inheritance, capabilities, and limits.
- `modules/economy.yml`: exact currencies in integer minor units.
- `modules/teams.yml`: the single team-name policy, invitation TTL, and friendly-fire setting.
- `modules/rewards.yml`: stable reward IDs, separate display text, weights, cooldown, and authoritative milestones.
- `messages.yml`: strict MiniMessage templates validated before activation.

Bad candidates do not replace the active runtime configuration. Admin changes use revision checks, validation, backup, temporary write, atomic replacement, commit-time capability checks, and audit records. Runtime-safe message-only reloads apply through `/magic reload`; provider, storage, rank, and gameplay changes are reported as restart-required.

## Phase 1 services

- Shared UUID profiles with bounded name history, locale, timestamps, and settings.
- Internal multi-currency economy with exact math, balances, payments, administrative adjustments, immutable ledger entries, and idempotency.
- Internal ranks, LuckPerms-owned membership, and hybrid LuckPerms membership with MagicCore perks/limits.
- Capabilities, inherited numeric limits, sync previews, and target-weight checks.
- Internal team creation, names, invitations, membership, roles, rank-based size limits, rename, leave, kick, and disband.
- Weighted daily rewards, streak state, authoritative sequential/independent playtime milestones, atomic economy credits, and replay protection.
- Native cancellable text-input sessions with no Skript/addon dependency.
- `/magic setup`, `/magic admin`, `/magic diagnose`, and `/magic reload` backends. No inventory GUI is included.
- Internal `%magiccore_*%` placeholder resolvers and an optional PlaceholderAPI expansion backed by the same cache.

## Vault behavior

External economy mode consumes a selected Vault provider through a persisted saga with compensation and reconciliation state. Registering MagicCore's internal async economy through Vault supports cached synchronous reads. Because Vault's mutation API is synchronous, MagicCore rejects tick-thread Vault mutations instead of blocking a Paper/Folia tick on durable storage; integrations requiring mutations should use MagicCore's asynchronous `EconomyService`. Diagnostics reports this bridge as degraded and actionable rather than silently weakening durability.

## Later work

Later phases are described in `PHASE_2.md` through `PHASE_7.md`.

Live Paper/Purpur/Folia smoke testing and live MariaDB/MongoDB conformance require external test environments. The reusable test contracts and environment-gated database tests are included, but a local `clean check` does not claim those unavailable systems were exercised.
