# PlayerInteractionsUtils Specification

## Original prompt
> * Create a `SPEC.md` (or README section) containing the original prompt and a checklist of required features (block tags, growth/transform tags, spawn-tag variants, last-hit timestamps, per-player counters, damage NBT tallies, config toggles, whitelist handling).
> * Add a lightweight test or validation step (e.g., a failing unit test placeholder or build check) that references this checklist so regressions are surfaced during CI.
> * Link the checklist from `plugin.yml`/project README so contributors review it when adding features.

## Feature checklist
These items **must remain covered** as the plugin evolves. Keep the checklist updated when adding or changing functionality.

- [x] **Block tags** are recorded for player-caused placements/breaks and persisted for later lookups.
- [x] **Growth/transform tags** capture block growth or transformation events initiated by players.
- [x] **Spawn-tag variants** track the player responsible for non-player entity spawns (e.g., eggs, buckets, spawners, natural with assistance).
- [x] **Last-hit timestamps** persist the most recent player damage time for tracked entities.
- [x] **Per-player counters** accumulate interaction metrics (blocks placed, growth/transform tags written, spawn tags, last-hit updates, damage records).
- [x] **Damage NBT tallies** store per-player damage amounts in entity NBT for auditing/rollback.
- [x] **Config toggles** exist to enable/disable each major feature without code changes.
- [x] **Whitelist handling** supports entity/tag allowlists to constrain tagging behavior.

## Checklist visibility
- `plugin.yml` and `README.md` link back to this document so contributors review the requirements before changing behavior.
- A JUnit test asserts this checklist remains present in `SPEC.md`, acting as a guardrail in CI.
