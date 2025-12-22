# Configuration notes

This plugin stores fallback disk data under `plugins/PlayerInteractionsUtils/` when the platform cannot persist `PersistentDataContainer` values across loads. The `storage` toggles in `config.yml` control whether those fallbacks are active.

- `storage.chunk-pdc-enabled`: When `true`, block metadata is stored directly in chunk PDC. When `false`, block metadata is **loaded on chunk load** from `chunk-blocks/` + `blocks/` and **saved on chunk unload/shutdown** so disk mirrors chunk “loaded/unloaded” state.
- `storage.entity-pdc-enabled`: When `true`, non-player entity metadata (spawn owners, damage tallies) lives in entity PDC. When `false`, non-player entities are **loaded when they spawn or a chunk loads** and **saved when they die or a chunk unloads/shutdown**, keeping disk state aligned with entity “loaded/unloaded” lifecycle.

Toggle one or both flags to verify the fallback paths engage: set the flag to `false`, reload the server, and watch for data files being read/written during chunk/entity load/unload operations instead of relying on PDC.
