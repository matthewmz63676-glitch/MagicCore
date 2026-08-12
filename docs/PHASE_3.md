# Phase 3 — Competitive economy

> Historical phase gate. Later-phase deferrals below describe the state at this gate; use `FEATURE_AUDIT.md` for current coverage.

## Auctions

- Listing preparation reserves the creator's rank-defined slot and configured fee before entity-thread inventory removal.
- The exact normalized item fingerprint and quantity are revalidated during removal. Successful escrow activation stores a recovery payload.
- Purchase re-reads the active, unexpired listing and commits buyer debit, seller credit, sold state, and buyer mailbox delivery in one storage transaction.
- Duplicate purchase keys cannot pay or deliver twice. Sellers cannot purchase their own listings.
- Cancellation and bounded expiry runs close the listing and atomically queue the escrowed item back to its seller.
- Categories, price bounds, listing fee, duration bounds, search, sorting, pagination, personal history, and slot limits are configurable/service-owned.

## Buy orders

- The buyer funds the entire order escrow at creation.
- Fulfillment reserves quantity before seller inventory removal, preventing concurrent fills from exceeding the remaining quantity.
- Partial/full settlement atomically credits the seller, advances authoritative quantities, decreases escrow, and queues exact items to the buyer.
- Rejected removals release reserved quantity. Cancellation and expiration require no active fulfillment and refund only unused escrow.
- Order categories, unit-price bounds, duration bounds, rank limits, history, and open-order views are service-owned.

## Bounties

- Creation rejects self-targeting, validates configured min/max values, and atomically debits escrow plus transparent basis-point tax.
- Multiple contributions aggregate under the target UUID with a configured contribution cap.
- Claims originate from the server's `PlayerDeathEvent` listener as a `VerifiedKill`; no reward command can claim a bounty.
- Kill IDs are durable idempotency keys. Claiming atomically closes contributions, credits the killer once, records history, and publishes `BountyClaimed` after commit.

## Recovery, analytics, and administration

- Auction/order items use the existing durable mailbox and entity-thread delivery markers. Full inventories stay pending.
- Marketplace analytics page through all records and expose active counts/value, escrow totals, sold volume, economy issuance/sinks/transfers, and balance leaderboards.
- Balance leaderboards merge known UUID profiles at their configured starting balance with persisted wallet overrides.
- Capability-gated marketplace administration exposes diagnostics and bounded auction/order expiry runs with audit records.
- `/magic auction`, `/magic order`, `/magic bounty`, and `/magic market` expose thin command clients. Future GUIs will call the same services.

## Verification and deferrals

`./gradlew clean check` passes with 61 tests: 59 executed and 2 live-database tests skipped. New tests prove atomic auction settlement and expiry recovery, partial order fulfillment/refund math, taxed bounty escrow and duplicate-kill rejection, and authoritative analytics.

Live Paper/Folia smoke tests and live MariaDB/MongoDB conformance remain environment-gated. Combat restrictions, kill anti-farming, protection-aware PvP, Lifesteal, and combat-tag behavior belong to Phase 4. Inventory GUI layouts remain deferred to Phase 7 and will contain presentation only.
